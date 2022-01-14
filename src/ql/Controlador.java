package ql;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;
import core.game.StateObservation;
import core.game.Observation;
import ontology.Types.ACTIONS;
import tools.Vector2d;

public class Controlador {

	Random randomGenerator;
	public static boolean metaDerecha = false;
	public static double distanciaJP = 1000000.0;
	public static int[][] contadorPasos;
	public static int distanciaAnterior = 10000;

	/* Estados definidos */
	public static enum ESTADOS {

		HUECO_DERECHA_OPTIMO(0), HUECO_IZQUIERDA_OPTIMO(0), HUECO_DERECHA(0), HUECO_IZQUIERDA(0), HUECO_ARRIBA(0),
		HUECO_ABAJO(0), OBSTACULO_ARRIBA(0), NIL(0);

		private int contador; // Determina el numero de veces que se encuentra en un estado

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

		// Devuelve el estado pasado por parametro
		public static ESTADOS buscaEstado(String nombreEstado) {
			for (ESTADOS s : ESTADOS.values()) {
				if (s.toString().equals(nombreEstado))
					return s;
			}

			return null;
		}
	}

	// Acciones
	public static final ACTIONS[] ACCIONES = { ACTIONS.ACTION_UP, ACTIONS.ACTION_DOWN, ACTIONS.ACTION_LEFT,
			ACTIONS.ACTION_RIGHT };

	// TABLA R
	public static HashMap<EstadoAccion, Double> R;
	// TABLA Q
	public static HashMap<EstadoAccion, Double> Q;
	private int numAcciones = ACCIONES.length;
	private int numEstados = ESTADOS.values().length;
	public static int numPartidasEntrenamiento;
	public static int partidaActual;

	public Controlador(boolean randomTablaQ) {
		System.out.println("Inicializando tablas Q y R.....");

		randomGenerator = new Random();
		inicializaTablaR();

		inicializaTablaQ(randomTablaQ);

	}

	public Controlador(String ficheroTablaQ) {

		System.out.println("Inicializando tablas Q y R.....");

		randomGenerator = new Random();
		inicializaTablaR();
		inicializaTablaQ(true);
		cargaTablaQ(ficheroTablaQ);

	}

// ---------------------------------------------------------------------
//  					METODOS TABLAS APRENDIZAJE
// ---------------------------------------------------------------------
	private void inicializaTablaR() {
		R = new HashMap<EstadoAccion, Double>(numEstados * numAcciones);

		double valorRecompensa = 0;

		for (ESTADOS e : ESTADOS.values()) {
			for (ACTIONS a : ACCIONES) {
				R.put(new EstadoAccion(e, a), valorRecompensa);
			}
		}

		R.put(new EstadoAccion(ESTADOS.HUECO_DERECHA_OPTIMO, ACTIONS.ACTION_RIGHT), 1000.0);
		R.put(new EstadoAccion(ESTADOS.HUECO_IZQUIERDA_OPTIMO, ACTIONS.ACTION_LEFT), 1000.0);

		R.put(new EstadoAccion(ESTADOS.HUECO_DERECHA, ACTIONS.ACTION_RIGHT), 100.0);
		R.put(new EstadoAccion(ESTADOS.HUECO_IZQUIERDA, ACTIONS.ACTION_LEFT), 100.0);
		R.put(new EstadoAccion(ESTADOS.HUECO_ABAJO, ACTIONS.ACTION_DOWN), 100.0);
		R.put(new EstadoAccion(ESTADOS.HUECO_ARRIBA, ACTIONS.ACTION_UP), 100.0);

		// penalizaciones
		R.put(new EstadoAccion(ESTADOS.OBSTACULO_ARRIBA, ACTIONS.ACTION_UP), -100.0);

	}

	/*
	 * Inializamos la TablaQ
	 */
	private void inicializaTablaQ(boolean random) {
		Q = new HashMap<EstadoAccion, Double>(numEstados * numAcciones);

		if (random) {
			/* Inicializamos los valores de la tablaQ a valores aleatorios */
			for (ESTADOS estado : ESTADOS.values())
				for (ACTIONS accion : ACCIONES)
					Q.put(new EstadoAccion(estado, accion), (randomGenerator.nextDouble() + 1) * 50);
		}

	}

	/**
	 * Por defecto utiliza TablaQ.csv .
	 */
	public void saveQTable() {
		saveQTable("TablaQ.csv");
	}

	/**
	 * Escribe sobre la tabla el estado y las acciones para realizar el seguimiento
	 * en base a la tablaQ
	 */
	public void saveQTable(String fileName) {
		/* Creación del fichero de salida */
		try (PrintWriter csvFile = new PrintWriter(new File(fileName))) {

			System.out.println(" Creando tablaQ ");

			StringBuilder buffer = new StringBuilder();
			buffer.append("ESTADOS");
			buffer.append(";");

			for (ACTIONS accion : Controlador.ACCIONES) {
				buffer.append(accion.toString());
				buffer.append(";");
			}

			buffer.append("\n");

			for (ESTADOS estado : ESTADOS.values()) {
				buffer.append(estado.toString());
				buffer.append(";");

				for (ACTIONS accion : Controlador.ACCIONES) {
					double value = Controlador.Q.get(new EstadoAccion(estado, accion));

					buffer.append('"' + Double.toString(value).replace('.', ',') + '"');
					buffer.append(";");
				}

				buffer.append("\n");
			}

			csvFile.write(buffer.toString());

			System.out.println(" Fichero tablaQ, creado correctamente");

			csvFile.close();

		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
	}

	private void cargaTablaQ(String filename) {

		/* Creación del fichero de salida */
		try (Scanner fichero = new Scanner(new File(filename));) {

			System.out.println(" Cargamos el fichero tabalQ " + filename);

			String linea = fichero.nextLine();
			String[] cabecera = linea.split(";");

			ACTIONS[] actions = new ACTIONS[cabecera.length];

			for (int i = 1; i < cabecera.length; i++) {
				for (ACTIONS a : ACCIONES) {

					System.out.println("Accion -> " + a.toString());
					if (a.toString().equals(cabecera[i])) {
						actions[i] = a;

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
					Q.put(new EstadoAccion(estado, actions[i]),
							Double.parseDouble(campos[i].replace(',', '.').replace('"', Character.MIN_VALUE)));

			}

			fichero.close();

		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
	}

	public static ACTIONS getAccionMaxQ(ESTADOS s) {
		ACTIONS[] actions = Controlador.ACCIONES;
		ACTIONS accionMaxQ = ACTIONS.ACTION_NIL;

		double maxValue = Double.NEGATIVE_INFINITY;

		for (int i = 0; i < actions.length; i++) {

			double value = Controlador.Q.get(new EstadoAccion(s, actions[i]));

			if (value > maxValue) {
				maxValue = value;
				accionMaxQ = actions[i];
			}
		}

		if (maxValue == 0) // Por si la creacion inicial de la tabla se hace rellenando con ceros
		{

			int index = new Random().nextInt(Controlador.ACCIONES.length - 1);
			accionMaxQ = actions[index];

		}

		return accionMaxQ;
	}

	public static char[][] getMapa(StateObservation so) {

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

		return diff;
	}

	public static ESTADOS getEstadoFuturo(StateObservation obs, ACTIONS action, int distanciaJP) {

		obs.advance(action);
		return getEstado(obs, getMapa(obs), distanciaJP);
	}

	public static ESTADOS getEstado(StateObservation obs, char[][] mapaObstaculos, int distanciaJP) {

		boolean avanza = false;
		if (Controlador.distanciaAnterior > distanciaJP) {
//			System.out.println("EL camello avanza");
			Controlador.distanciaAnterior = distanciaJP;
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

				}

				if (estadoEspacioFilasDerecha == 1) {

					// el mejor esta arriba

					if (mapa[xCamello][yCamello - 1] != '#') {
//						System.out.println("Hueco Arriba");
						return ESTADOS.HUECO_ARRIBA;
					}
					if (mapa[xCamello + 1][yCamello] != '#') {
//						System.out.println("Hueco derecha");
						return ESTADOS.HUECO_DERECHA;
					}

//					

				}

				if (estadoEspacioFilasDerecha == 2) {
					if (mapa[xCamello][yCamello + 1] != '#') {
//						System.out.println("Hueco Abajo");
						return ESTADOS.HUECO_ABAJO;
					}

					if (mapa[xCamello + 1][yCamello] != '#') {
//						System.out.println("Hueco derecha");
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

				}

				if (estadoEspacioFilasIzquierda == 1) {

					// orden inicial hueco arriba despues hueco derecha asi funciona

					if (mapa[xCamello - 1][yCamello] != '#') {
//						System.out.println("Hueco izquierda");
						return ESTADOS.HUECO_IZQUIERDA;
					}

					if (mapa[xCamello][yCamello - 1] != '#') {
//						System.out.println("Hueco Arriba");
						return ESTADOS.HUECO_ARRIBA;
					}

				}

				if (estadoEspacioFilasIzquierda == 2) {
					if (mapa[xCamello][yCamello + 1] != '#') {
//						System.out.println("Hueco Abajo");
						return ESTADOS.HUECO_ABAJO;
					}

					if (mapa[xCamello - 1][yCamello] != '#') {
//						System.out.println("Hueco derecha");
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
//			System.out.println("Estas fila mejor");
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
//			System.out.println("La fila mejor esta abajo");
			return 2;
		} else if (indiceFilaMejor < yCamello) {
//			System.out.println("La fila mejor esta arriba");
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
//			System.out.println("Estas fila mejor");
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

		// Compruebo si la fila del camello tine huecos iguales al mejor, es decir,
		// puede ser el mejor
		if (indiceFilaMejor == yCamello) {
			return 0;
		}

		if (indiceFilaMejor > yCamello) {
//			System.out.println("La fila mejor esta abajo");
			return 2;
		} else if (indiceFilaMejor < yCamello) {
//			System.out.println("La fila mejor esta arriba");
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

		if (dis < Controlador.distanciaJP) {
			Controlador.distanciaJP = dis;
		}

	}

	public void getContadoresEstados() {
		System.out.println(" REPETICIONES POR ESTADOS ");
		for (ESTADOS s : ESTADOS.values()) {

			System.out.println(s.toString() + " : " + s.getContador());
		}
	}

	public static void pintaQTable(ESTADOS s) {
		ACTIONS[] actions = Controlador.ACCIONES;

		System.out.println(" TABLA-Q");

		for (int i = 0; i < actions.length; i++) {
			System.out.print("Actual Q<" + s.toString() + ",");
			System.out.print(actions[i] + "> = ");

			double value = Controlador.Q.get(new EstadoAccion(s, actions[i]));

			System.out.println(value);
		}

	}

	public static void pintaQTableResumen() {

		ESTADOS[] estados = ESTADOS.values();

		System.out.println("Tabla Q detalles");

		for (int i = 0; i < estados.length; i++) {
			ACTIONS accion = getAccionMaxQ(estados[i]);
			double q = Controlador.Q.get(new EstadoAccion(estados[i], accion));

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