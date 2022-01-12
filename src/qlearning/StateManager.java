package qlearning;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;
import java.lang.Thread.State;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Scanner;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.swing.Action;

import core.game.StateObservation;
import core.game.Observation;
import ontology.Types.ACTIONS;
import tools.Vector2d;

public class StateManager {
	public static boolean verbose = false;
	// Variables simulacion training
	public static int numIteraciones;
	public static int iteracionActual;
	Random randomGenerator;
	public static boolean metaDerecha = false;
	// Variables comunes a varias clases
	public static int numCol;
	public static int numFilas;
	public static double distanciaJP = 1000000.0;
	public static int[][] contadorPasos;
	public static int xJugador;
	public static int yJugador;

	public static int distanciaAnterior = 10000;

	/* Contenedor de constantes para identificar los estados */
	public static enum ESTADOS {

		HUECO_DERECHA_OPTIMO(0), HUECO_IZQUIERDA_OPTIMO(0), HUECO_DERECHA(0), HUECO_IZQUIERDA(0), HUECO_ARRIBA(0),
		HUECO_ABAJO(0), OBSTACULO_ARRIBA(0), NIL(0);

		private int contador; // Cuenta cada vez que se percibe ese estado

		ESTADOS(int c) {
			this.contador = c;
		}

		ESTADOS() {
			this.contador = 0;
		}

		public void incrementa() {
			this.contador++;
		}

		public int getContador() {
			return this.contador;
		}

		// Devuelve el enum ESTADOS al que se corresponde la cadena pasada por parametro
		public static ESTADOS buscaEstado(String nombreEstado) {
			for (ESTADOS s : ESTADOS.values()) {
				if (s.toString().equals(nombreEstado))
					return s;
			}

			return null;
		}
	}

	// Acciones posibles
	public static final ACTIONS[] ACCIONES = { ACTIONS.ACTION_UP, ACTIONS.ACTION_DOWN, ACTIONS.ACTION_LEFT,
			ACTIONS.ACTION_RIGHT };

	public static HashMap<ParEstadoAccion, Double> R; // TABLA R
	public static HashMap<ParEstadoAccion, Double> Q; // TABLA Q

	/* Variables */
	// private static char mapaObstaculos[][];
	private static int posActual[];
	private int numEstados = ESTADOS.values().length;
	private int numAcciones = ACCIONES.length;

	public StateManager(boolean randomTablaQ, boolean verbose) {
		if (verbose)
			System.out.println("Inicializando tablas Q y R.....");

		randomGenerator = new Random();
		inicializaTablaR();

		inicializaTablaQ(randomTablaQ);

		StateManager.verbose = verbose;

	}

	public StateManager(String ficheroTablaQ, boolean verbose) {
		if (verbose)
			System.out.println("Inicializando tablas Q y R.....");

		randomGenerator = new Random();
		inicializaTablaR();
		inicializaTablaQ(true);
		cargaTablaQ(ficheroTablaQ);

		StateManager.verbose = verbose;
	}

// ---------------------------------------------------------------------
//  					METODOS TABLAS APRENDIZAJE
// ---------------------------------------------------------------------
	private void inicializaTablaR() {
		R = new HashMap<ParEstadoAccion, Double>(numEstados * numAcciones);

		double valorRecompensa = 0;

		for (ESTADOS e : ESTADOS.values()) {
			for (ACTIONS a : ACCIONES) {
				R.put(new ParEstadoAccion(e, a), valorRecompensa);
			}
		}

		R.put(new ParEstadoAccion(ESTADOS.HUECO_DERECHA_OPTIMO, ACTIONS.ACTION_RIGHT), 1000.0);
		R.put(new ParEstadoAccion(ESTADOS.HUECO_IZQUIERDA_OPTIMO, ACTIONS.ACTION_LEFT), 1000.0);

		R.put(new ParEstadoAccion(ESTADOS.HUECO_DERECHA, ACTIONS.ACTION_RIGHT), 100.0);
		R.put(new ParEstadoAccion(ESTADOS.HUECO_IZQUIERDA, ACTIONS.ACTION_LEFT), 100.0);
		R.put(new ParEstadoAccion(ESTADOS.HUECO_ABAJO, ACTIONS.ACTION_DOWN), 100.0);
		R.put(new ParEstadoAccion(ESTADOS.HUECO_ARRIBA, ACTIONS.ACTION_UP), 100.0);

		// penalizaciones
		R.put(new ParEstadoAccion(ESTADOS.OBSTACULO_ARRIBA, ACTIONS.ACTION_UP), -100.0);

		

	}

	/*
	 * Inializamos la TablaQ
	 */
	private void inicializaTablaQ(boolean random) {
		Q = new HashMap<ParEstadoAccion, Double>(numEstados * numAcciones);

		if (random) {
			/* Inicializamos todos los valores Q a random */
			for (ESTADOS estado : ESTADOS.values())
				for (ACTIONS accion : ACCIONES)
					Q.put(new ParEstadoAccion(estado, accion), (randomGenerator.nextDouble() + 1) * 50);
			// Q.put(new ParEstadoAccion(estado, accion), (randomGenerator.nextDouble() *
			// 0.2));
		} else {
			/* Inicializamos todos los valores Q a cero */
			for (ESTADOS estado : ESTADOS.values())
				for (ACTIONS accion : ACCIONES) {
					Q.put(new ParEstadoAccion(estado, accion), 0.0);
					// System.out.println(estado.toString() + "," + accion.toString() + " = 0.0");
				}
		}

	}

	/**
	 * Si no le indicamos el nombre del fichero, usa uno por defecto.
	 */
	public void saveQTable() {
		saveQTable("TablaQ.csv");
	}

	/**
	 * Escribe la tabla Q del atributo de la clase en el fichero QTable.csv, para
	 * poder ser leída en una siguiente etapa de aprendizaje.
	 */
	public void saveQTable(String fileName) {
		/* Creación del fichero de salida */
		try (PrintWriter csvFile = new PrintWriter(new File(fileName))) {

			if (verbose)
				System.out.println(" GENERANDO EL FICHERO DE LA TABLAQ... ");

			StringBuilder buffer = new StringBuilder();
			buffer.append("ESTADOS");
			buffer.append(";");

			for (ACTIONS accion : StateManager.ACCIONES) {
				buffer.append(accion.toString());
				buffer.append(";");
			}

			buffer.append("\n");

			for (ESTADOS estado : ESTADOS.values()) {
				buffer.append(estado.toString());
				buffer.append(";");

				for (ACTIONS accion : StateManager.ACCIONES) {
					double value = StateManager.Q.get(new ParEstadoAccion(estado, accion));

					buffer.append('"' + Double.toString(value).replace('.', ',') + '"');
					buffer.append(";");
				}

				buffer.append("\n");
			}

			csvFile.write(buffer.toString());

			if (verbose)
				System.out.println(" FICHERO GENERADO CORRECTAMENTE! ");

			csvFile.close();

		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
	}

	private void cargaTablaQ(String filename) {

		/* Creación del fichero de salida */
		try (Scanner fichero = new Scanner(new File(filename));) {

			if (verbose)
				System.out.println(" CARGANDO EL FICHERO DE LA TABLAQ: " + filename);

			String linea = fichero.nextLine();
			String[] cabecera = linea.split(";");

			ACTIONS[] actions = new ACTIONS[cabecera.length];

			for (int i = 1; i < cabecera.length; i++) {
				for (ACTIONS a : ACCIONES) {
					if (verbose)
						System.out.println("NOMBRE ACCION: " + a.toString());
					if (a.toString().equals(cabecera[i])) {
						actions[i] = a;
						if (verbose)
							System.out.println(actions[i] + " = " + a.toString());
						break;
					}
				}
			}

			while (fichero.hasNextLine()) {
				linea = fichero.nextLine();

				String[] campos = linea.split(";");

				// Según el estado
				ESTADOS estado = ESTADOS.buscaEstado(campos[0]);

				// Por cada celda, le metemos el valor Q reemplazando coma por punto
				for (int i = 1; i < campos.length; i++)
					Q.put(new ParEstadoAccion(estado, actions[i]),
							Double.parseDouble(campos[i].replace(',', '.').replace('"', Character.MIN_VALUE)));

			}

			fichero.close();

		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
	}

	public static ACTIONS getAccionMaxQ(ESTADOS s) {
		ACTIONS[] actions = StateManager.ACCIONES; // Acciones posibles
		ACTIONS accionMaxQ = ACTIONS.ACTION_NIL;

		double maxValue = Double.NEGATIVE_INFINITY; // - inf

		for (int i = 0; i < actions.length; i++) {

			// if(verbose) System.out.print("Actual maxQ<"+ s.toString() + "," );
			// if(verbose) System.out.print(actions[i]+"> = ");
			double value = StateManager.Q.get(new ParEstadoAccion(s, actions[i]));
			// if(verbose) System.out.println(value);

			if (value > maxValue) {
				maxValue = value;
				accionMaxQ = actions[i];
			}
		}

		if (maxValue == 0) // Inicialmente estan a 0, una random
		{

			int index = new Random().nextInt(StateManager.ACCIONES.length - 1);
			accionMaxQ = actions[index];

		}

		return accionMaxQ;
	}

// _____________________________________________________________________
//  METODOS PERCEPCION ESTADOS
//_____________________________________________________________________

	public static char[][] getMapa(StateObservation so) {

//		int numCol = so.getWorldDimension().width / so.getBlockSize();
//		int numFil = so.getWorldDimension().height / so.getBlockSize();
//
//		char[][] mapa = new char[numCol][numFil];
//		for (int i = 0; i < numFil; i++) {
//			for (int j = 1; j < numCol - 1; j++) {
//
//				if (so.getObservationGrid()[j][i].size() != 0) {
//					if (so.getObservationGrid()[j][i].get(0).itype == 0) {
//						mapa[j][i] = '#';
//					} else if (so.getObservationGrid()[j][i].get(0).category == 2) {
//						mapa[j][i] = 'M';
//					} else if (so.getObservationGrid()[j][i].get(0).category == 0) {
//						mapa[j][i] = 'J';
//					}
//					else {
//						mapa[j][i] = '-';
//					}
//				} else {
//					mapa[j][i] = '-';
//				}
//
//			}
//		}
//
//		return mapa;

		char[][] res = new char[so.getWorldDimension().width / so.getBlockSize()][so.getWorldDimension().height
				/ so.getBlockSize()];

		for (int j = 0; j < so.getObservationGrid()[0].length; j++) {

			for (int i = 1; i < so.getObservationGrid().length; i++) {
				if (so.getObservationGrid()[i][j].size() != 0) {
					if (so.getObservationGrid()[i][j].get(0).category == 4) {
						res[i][j] = '#';
					} else if (so.getObservationGrid()[i][j].get(0).category == 0) {
						res[i][j] = 'J';
					} else if (so.getObservationGrid()[i][j].get(0).category == 2) {
						res[i][j] = 'M';
					} else {
						res[i][j] = '-';
					}
				} else {
					res[i][j] = '-';
				}

			}
			System.out.println();
		}
		return res;

	}

	public static int distanciaMeta(StateObservation obs) {
		double posJugador[] = getPosicionJugador(obs);
		ArrayList<Observation>[] portalPosition = obs.getPortalsPositions();

		int Xjugador = (int) posJugador[0];
		int Xportal = (int) (portalPosition[0].get(0).position.x / obs.getBlockSize());

		int diff = Math.abs((Xjugador - Xportal));
		// System.out.println("Distancia JP " + diff);

		return diff;
	}

	public static ESTADOS getEstadoFuturo(StateObservation obs, ACTIONS action, int distanciaJP) {

		obs.advance(action);
		return getEstado(obs, getMapa(obs), distanciaJP);
	}

	public static ESTADOS getEstado(StateObservation obs, char[][] mapaObstaculos, int distanciaJP) {

		boolean avanza = false;
		if (StateManager.distanciaAnterior > distanciaJP) {
//			System.out.println("EL camello avanza");
			StateManager.distanciaAnterior = distanciaJP;
			avanza = true;
		} else {
			avanza = false;
//			System.out.println("El camello no avanza");
		}

		ArrayList<Observation>[] portalPosition = obs.getPortalsPositions();
		double posicionCamello[] = getPosicionJugador(obs);

		int yCamello = (int) posicionCamello[1];
		int xCamello = (int) posicionCamello[0];

		if (portalPosition[0].get(0).position.x > 20) {
			// portal se encuentra en la derecha
			metaDerecha = true;
		} else {
			metaDerecha = false;
		}

		if (!obs.isGameOver()) {
			char[][] mapa = getMapa(obs);
			int estadoEspacioFilasDerecha = caminoMayorDesplazamientoDerecha(obs);
			int estadoEspacioFilasIzquierda = caminoMayorDesplazamientoIzquieda(obs);
			// hueco derecha
			if (metaDerecha) {

				if (estadoEspacioFilasDerecha == 0) {

					if (mapa[xCamello + 1][yCamello] != '#') {
//						System.out.println("Hueco derecha");
						return ESTADOS.HUECO_DERECHA_OPTIMO;
					}
					if (mapa[xCamello + 1][yCamello] == '#' && mapa[xCamello][yCamello - 1] != '#') {
						return ESTADOS.HUECO_ARRIBA;
//						System.out.println("Obstaculo derecha y hueco arriba");
					}
					if (mapa[xCamello + 1][yCamello] == '#' && mapa[xCamello][yCamello + 1] != '#') {
						return ESTADOS.HUECO_ABAJO;
//						System.out.println("Obstaculo derecha  y hueco abajo");
					}
					// faltaria caso donde obstaculo arriba derecha y abajo ??? pensar

				}

				if (estadoEspacioFilasDerecha == 1) {

					// el mejor esta arriba

					if (mapa[xCamello][yCamello - 1] != '#') {
						System.out.println("Hueco Arriba");
						return ESTADOS.HUECO_ARRIBA;
					}
					if (mapa[xCamello + 1][yCamello] != '#') {
						System.out.println("Hueco derecha");
						return ESTADOS.HUECO_DERECHA;
					}

//					

				}

				if (estadoEspacioFilasDerecha == 2) {
					if (mapa[xCamello][yCamello + 1] != '#') {
						System.out.println("Hueco Abajo");
						return ESTADOS.HUECO_ABAJO;
					}

					if (mapa[xCamello + 1][yCamello] != '#') {
						System.out.println("Hueco derecha");
						return ESTADOS.HUECO_DERECHA;
					}

				}

			}

			if (!metaDerecha) {

				if (estadoEspacioFilasIzquierda == 0) {

					if (mapa[xCamello - 1][yCamello] != '#') {
//						System.out.println("Hueco izquierda");
						return ESTADOS.HUECO_IZQUIERDA_OPTIMO;
					}
					if (mapa[xCamello - 1][yCamello] == '#' && mapa[xCamello][yCamello - 1] != '#') {
						return ESTADOS.HUECO_ARRIBA;
//						System.out.println("Obstaculo izquierda y hueco arriba");
					}
					if (mapa[xCamello - 1][yCamello] == '#' && mapa[xCamello][yCamello + 1] != '#') {
						return ESTADOS.HUECO_ABAJO;
//						System.out.println("Obstaculo izquierda  y hueco abajo");
					}
					// faltaria caso donde obstaculo arriba derecha y abajo ??? pensar

				}

				if (estadoEspacioFilasIzquierda == 1) {

					// orden inicial hueco arriba despues hueco derecha asi funciona

					if (mapa[xCamello - 1][yCamello] != '#') {
						System.out.println("Hueco izquierda");
						return ESTADOS.HUECO_IZQUIERDA;
					}

					if (mapa[xCamello][yCamello - 1] != '#') {
						System.out.println("Hueco Arriba");
						return ESTADOS.HUECO_ARRIBA;
					}

					// add posterior
//					if (mapa[xCamello][yCamello + 1] != '#') {
//						System.out.println("Hueco abajo");
//						return ESTADOS.HUECO_ABAJO;
//					}

				}

				if (estadoEspacioFilasIzquierda == 2) {
					if (mapa[xCamello][yCamello + 1] != '#') {
						System.out.println("Hueco Abajo");
						return ESTADOS.HUECO_ABAJO;
					}

					if (mapa[xCamello - 1][yCamello] != '#') {
						System.out.println("Hueco derecha");
						return ESTADOS.HUECO_IZQUIERDA;
					}

				}

			}

		}

		return ESTADOS.NIL;

	}

	public static int caminoMayorDesplazamientoDerecha(StateObservation so) {

		// Si ya esta en la mejor fila 0
		// Si la fila mejor esta en una fila menor 1 // hacia arriba
		// Si la fila mejor esta en una fila mayor 2 // hacia abajo
		int estado = 0;
		double posicionCamello[] = getPosicionJugador(so);

		char mapa[][] = getMapa(so);
		// Compruebo si si la fila donde estoy hay 0 obstaculos es ya la mejor
		int yCamello = (int) posicionCamello[1];
		int xCamello = (int) posicionCamello[0];
		int contadorEspacios = 0;
		int contadorObstaculos = 0;
//		System.out.println(xCamello + "  "+ yCamello);
		// Compruebo si estoy ya en una fila donde no hay obstaculos
		for (int i = xCamello; i < mapa.length - 1; i++) {
			if (mapa[i][yCamello] == '#') {
				contadorObstaculos++;
			}
		}
		if (contadorObstaculos == 0) {
			System.out.println("Estas fila mejor");
			return 0;
		}

		// Guardo el numero de obstaculos por fila
		int espaciosFila[] = new int[7];
		for (int fila = 1; fila < 8; fila++) {
			contadorEspacios = 0;
			int i = xCamello + 1;

			while (mapa[i][fila] != '#' && i < mapa.length - 1) {
				contadorEspacios++;
				i++;
			}

			espaciosFila[fila - 1] = contadorEspacios;
		}

		int indiceFilaMejor = 0;
		int aux = -1;
		for (int i = 0; i < espaciosFila.length; i++) {
			if (espaciosFila[i] > aux) {
				indiceFilaMejor = i;
				aux = espaciosFila[i];
			}
		}
		indiceFilaMejor++;
//		System.out.println("Fila menor: " + indiceFilaMenor);

		// Compruebo si la fila del camello tine huecos iguales al mejor, es decir,
		// puede ser el mejor
		if (indiceFilaMejor == yCamello) {
			return 0;
		}

		if (indiceFilaMejor > yCamello) {
			System.out.println("La fila mejor esta abajo");
			return 2;
		} else if (indiceFilaMejor < yCamello) {
			System.out.println("La fila mejor esta arriba");
			return 1;
		}

		return -1;
	}

	public static int caminoMayorDesplazamientoIzquieda(StateObservation so) {

		// Si ya esta en la mejor fila 0
		// Si la fila mejor esta en una fila menor 1 // hacia arriba
		// Si la fila mejor esta en una fila mayor 2 // hacia abajo
		int estado = 0;
		double posicionCamello[] = getPosicionJugador(so);

		char mapa[][] = getMapa(so);
		// Compruebo si si la fila donde estoy hay 0 obstaculos es ya la mejor
		int yCamello = (int) posicionCamello[1];
		int xCamello = (int) posicionCamello[0];
		int contadorEspacios = 0;
		int contadorObstaculos = 0;
//		System.out.println(xCamello + "  "+ yCamello);
		// Compruebo si estoy ya en una fila donde no hay obstaculos
		for (int i = xCamello; i > 0; i--) {
			if (mapa[i][yCamello] == '#') {
				contadorObstaculos++;
			}
		}
		if (contadorObstaculos == 0) {
			System.out.println("Estas fila mejor");
			return 0;
		}

		// Guardo el numero de obstaculos por fila
		int espaciosFila[] = new int[7];
		for (int fila = 1; fila < 8; fila++) {
			contadorEspacios = 0;
			int i = xCamello - 1;

			while (mapa[i][fila] != '#' && i < 0) {
				contadorEspacios++;
				i--;
			}

			espaciosFila[fila - 1] = contadorEspacios;
		}

		int indiceFilaMejor = 0;
		int aux = -1;
		for (int i = 0; i < espaciosFila.length; i++) {
			if (espaciosFila[i] > aux) {
				indiceFilaMejor = i;
				aux = espaciosFila[i];
			}
		}
		indiceFilaMejor++;
//		System.out.println("Fila menor: " + indiceFilaMenor);

		// Compruebo si la fila del camello tine huecos iguales al mejor, es decir,
		// puede ser el mejor
		if (indiceFilaMejor == yCamello) {
			return 0;
		}

		if (indiceFilaMejor > yCamello) {
			System.out.println("La fila mejor esta abajo");
			return 2;
		} else if (indiceFilaMejor < yCamello) {
			System.out.println("La fila mejor esta arriba");
			return 1;
		}

		return -1;
	}

	public static double[] getPosicionJugador(StateObservation so) {

		Vector2d pos = so.getAvatarPosition();
		double posicionJugador[] = new double[2];
		posicionJugador[0] = pos.x / 16;
		posicionJugador[1] = pos.y / 16;
		// System.out.println(pos.x / 16 + " " + pos.y / 16 + " ori ");
		return posicionJugador;
	}

	public static void setDistanciaJugadorPortal(StateObservation so) {
		Observation portal = so.getPortalsPositions()[0].get(0);
		double xPortal = portal.position.x / 16;

		double[] posJufador = getPosicionJugador(so);

		double dis = Math.abs((xPortal - posJufador[0]));

		if (dis < StateManager.distanciaJP) {
			StateManager.distanciaJP = dis;
		}

	}

	public static int filaMasCaminoFull(StateObservation so, double posicionJugador[]) {
		double filaJugador = posicionJugador[1];
		int columnaJugador = (int) posicionJugador[0];
		char[][] mapa = getMapa(so);
		int indiceMaxDist = -1; // indice de la fila con mayor distancia
		int numColMapa = so.getWorldDimension().width / so.getBlockSize();
		int numFilMapa = so.getWorldDimension().height / so.getBlockSize();
		int maxDist = -99;
		// bucle esta mal
		for (int j = columnaJugador; j < numColMapa; j++) {
			for (int i = 0; i < numFilMapa; i++) {
				if (i > -1) {
					if (mapa[j][i] == '#') {
						if (j - columnaJugador > maxDist) {
							indiceMaxDist = i;
							maxDist = j - columnaJugador;
						}
					}
				}
			}
		}
		return indiceMaxDist + 1; // no se si hacer -1 por el i++ del for
	}

	// mira solo la de arriba yla de abjo
	public static int filaMasCamino(StateObservation so, double posicionJugador[]) {
		int filaJugador = (int) posicionJugador[1];
		int columnaJugador = (int) posicionJugador[0];
		char[][] mapa = getMapa(so);
		int indiceMaxDist = -1; // indice de la fila con mayor distancia
		int numColMapa = so.getWorldDimension().width / so.getBlockSize();
		int numFilMapa = so.getWorldDimension().height / so.getBlockSize();
		int maxDist = -99;

		// mapa[columna][fila]
		// replantear el bucle
		for (int j = columnaJugador; j < numColMapa; j++) {
			for (int i = filaJugador - 1; i <= filaJugador + 1; i++) {
				if (i > -1) {
					if (mapa[j][i] == '#') {
						if (j - columnaJugador > maxDist) {
							indiceMaxDist = i;
							maxDist = j - columnaJugador;
						}
					}
				}
			}
		}
		return indiceMaxDist + 1; // no se si hacer -1 por el i++ del for
	}

	public void getContadoresEstados() {
		System.out.println("____________ CONTADORES ESTADOS _____________________");
		for (ESTADOS s : ESTADOS.values()) {

			System.out.println(s.toString() + " : " + s.getContador());
		}
	}

// _____________________________________________________________________
//                    METODOS PERCEPCION MAPA
// _____________________________________________________________________

//	public static char[][] getMapaObstaculos(StateObservation obs)
//	{
//		// El desplazamiento de un jugador es en 0.5 casillas
//		char[][] mapaObstaculos = new char[numFilas*2][numCol*2];
//		
//		for(int i=0; i<numFilas*2; i++)
//			for(int j=0; j<numCol*2; j++)
//				mapaObstaculos[i][j] = ' ';
//		
//		
//	    	for(ArrayList<Observation> lista : obs.getMovablePositions())
//	    		for(Observation objeto : lista)
//	    		{
//	    			
//	    			double[] pos = getCeldaPreciso(objeto.position, obs.getWorldDimension()); // Posicion en casilla real 0.5
//	    			int [] indicePos = getIndiceMapa(pos); // Indice del mapa
//	    		
//	    			
//	    			//System.out.println("Objeto en " + pos[0] + "-" + pos[1] + " = "+ objeto.itype + " REAL: " + objeto.position.toString());
//	    			//System.out.println(this.mapaObstaculos[pos[0]][pos[1]]);
//	    			
//	    			switch(objeto.itype)
//					{    					
//						case 1:
//							mapaObstaculos[indicePos[0]][indicePos[1]] = 'O';
//							break;
//						case 10:
//						case 16:
//							mapaObstaculos[indicePos[0]][indicePos[1]] = '|';
//							break;
//						case 6:
//						case 7:
//						case 13:
//						case 14:
//							mapaObstaculos[indicePos[0]][indicePos[1]] = 'X';
//							break;
//						case 9:
//						case 15:
//							mapaObstaculos[indicePos[0]][indicePos[1]] = 'G';
//							break; 
//						default:
//							mapaObstaculos[indicePos[0]][indicePos[1]] = '.';
//							break;
//	    		}
//			}
//    	
//    	return mapaObstaculos;
//	}

// _____________________________________________________________________
//  METODOS VISUALES
//_____________________________________________________________________	
	public static void pintaQTable(ESTADOS s) {
		ACTIONS[] actions = StateManager.ACCIONES;

		System.out.println("----------Q TABLE -----------------");

		for (int i = 0; i < actions.length; i++) {
			System.out.print("Actual Q<" + s.toString() + ",");
			System.out.print(actions[i] + "> = ");

			double value = StateManager.Q.get(new ParEstadoAccion(s, actions[i]));

			System.out.println(value);
		}

		System.out.println("----------Q TABLE -----------------");
	}

	public static void pintaQTableResumen() {

		ESTADOS[] estados = ESTADOS.values();

		System.out.println("____________________ Q TABLE RESUMEN ______________________");

		for (int i = 0; i < estados.length; i++) {
			ACTIONS accion = getAccionMaxQ(estados[i]);
			double q = StateManager.Q.get(new ParEstadoAccion(estados[i], accion));

			System.out.println("maxQ<" + estados[i].toString() + "," + accion.toString() + "> = " + q);

		}

		System.out.println("_________________________________________________________");
	}

	public static void showMapa(StateObservation so) {

		int numCol = so.getWorldDimension().width / so.getBlockSize();
		int numFil = so.getWorldDimension().height / so.getBlockSize();
		char[][] mapa = getMapa(so);

		for (int i = 0; i < numFil; i++) {
			for (int j = 0; j < numCol; j++) {
				System.out.print(mapa[j][i]);
			}
			System.out.println();
		}
	}

}