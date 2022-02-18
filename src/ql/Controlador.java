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

	public static double distanciaJugadortoChungo = Double.MAX_VALUE;
	public static double distanciaMenorJugadorInfectado = Double.MAX_VALUE;
	public static int numeroInfectados = Integer.MAX_VALUE;

	/* Estados definidos */
	public static enum ESTADOS {

		POSICION_PERFECTA(0), POSICION_SEGURA(0), POSICION_NO_SEGURA(0), TRANSICION_POSITIVA(0), TRANSICION_NEGATIVA(0),
		REDUCE_DIST_INFECTADO(0), AUMENTA_DIST_INFECTADO(0), REDUCE_INFECTADOS(0), INFECTADO_CERCA(0), NIL(0);

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
	public static final ACTIONS[] ACCIONES = { ACTIONS.ACTION_USE, ACTIONS.ACTION_UP, ACTIONS.ACTION_DOWN,
			ACTIONS.ACTION_LEFT, ACTIONS.ACTION_RIGHT };

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

		for (ACTIONS a : ACCIONES) {
			R.put(new EstadoAccion(ESTADOS.POSICION_PERFECTA, a), 100.0);
			R.put(new EstadoAccion(ESTADOS.POSICION_SEGURA, a), 20.0);
			R.put(new EstadoAccion(ESTADOS.POSICION_NO_SEGURA, a), -30.0);
			R.put(new EstadoAccion(ESTADOS.TRANSICION_POSITIVA, a), 50.0);
			R.put(new EstadoAccion(ESTADOS.TRANSICION_NEGATIVA, a), -300.0);
			R.put(new EstadoAccion(ESTADOS.REDUCE_DIST_INFECTADO, a), 20.0);
			R.put(new EstadoAccion(ESTADOS.AUMENTA_DIST_INFECTADO, a), -20.0);
		}

		R.put(new EstadoAccion(ESTADOS.INFECTADO_CERCA, ACTIONS.ACTION_USE), 200.0);
		R.put(new EstadoAccion(ESTADOS.INFECTADO_CERCA, ACTIONS.ACTION_DOWN), -10.0);
		R.put(new EstadoAccion(ESTADOS.INFECTADO_CERCA, ACTIONS.ACTION_UP), -10.0);
		R.put(new EstadoAccion(ESTADOS.INFECTADO_CERCA, ACTIONS.ACTION_RIGHT), -10.0);
		R.put(new EstadoAccion(ESTADOS.INFECTADO_CERCA, ACTIONS.ACTION_LEFT), -10.0);
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

		// itype 0 -> muros
		// itype -> 1 Jugador
		// itype -> 4 personaje infectado
		// itype -> 6 personaje bueno
		// itype -> 7 tio chungo

		char[][] res = new char[so.getWorldDimension().width / so.getBlockSize()][so.getWorldDimension().height
				/ so.getBlockSize()];

		for (int j = 0; j < so.getObservationGrid()[0].length; j++) {

			for (int i = 0; i < so.getObservationGrid().length; i++) {
				if (so.getObservationGrid()[i][j].size() != 0) {
					if (so.getObservationGrid()[i][j].get(0).itype == 0) {
						res[i][j] = '#';
					} else if (so.getObservationGrid()[i][j].get(0).itype == 1) {
						res[i][j] = 'J';
					} else if (so.getObservationGrid()[i][j].get(0).itype == 4) {
						res[i][j] = 'E';
					} else if (so.getObservationGrid()[i][j].get(0).itype == 6) {
						res[i][j] = 'S';
					} else if (so.getObservationGrid()[i][j].get(0).itype == 7) {
						res[i][j] = 'P';
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

	public static ESTADOS getEstadoFuturo(StateObservation obs, ACTIONS action, int numeroInfectados,
			double distanciaJugadortoChungo, double distanciaMenorJugadorInfectado) {

		obs.advance(action);
		return getEstado(obs, getMapa(obs), numeroInfectados, distanciaJugadortoChungo, distanciaMenorJugadorInfectado);
	}

	public static ESTADOS getEstado(StateObservation obs, char[][] mapaObstaculos, int numeroInfectados,
			double distanciaJugadortoChungo, double distanciaMenorJugadorInfectado) {

		ESTADOS estadoFinal;

		// num de infectados actuales tras la transicion
		int numeroInfectadosActuales = numeroInfectados(obs);
		double distanciaInfectadoActual = getDistanciaJugadorNPCCercano(obs);
		double distanciatoChungo = getDistanciaJugadorNPCCercano(obs);

		if (numeroInfectadosActuales < numeroInfectados) {
			//if(sentido es igual al infectado then 
			if (distanciaInfectadoActual < 5) {
				return ESTADOS.INFECTADO_CERCA;
			}
			return ESTADOS.TRANSICION_POSITIVA;
		} else {
			if (numeroInfectadosActuales > numeroInfectados) {
				if (distanciaInfectadoActual < 5) {
					return ESTADOS.INFECTADO_CERCA;
				}
				return ESTADOS.TRANSICION_NEGATIVA;
			} else {

				if (distanciaInfectadoActual < distanciaMenorJugadorInfectado && distanciatoChungo > 3) {
					// ha reducido la dist entre infectado y jugador y mantiene alejado al chungo
					if (distanciaInfectadoActual < 5) {
						return ESTADOS.INFECTADO_CERCA;
					}
					return ESTADOS.TRANSICION_POSITIVA;
				}

				if (distanciaInfectadoActual >= distanciaMenorJugadorInfectado || distanciatoChungo < 3) {
					if (distanciaInfectadoActual < 5) {
						return ESTADOS.INFECTADO_CERCA;
					}
					return ESTADOS.TRANSICION_NEGATIVA;
				}

			}

		}

		return ESTADOS.NIL;

	}

	public static double getDistanciaJugadorNPCCercano(StateObservation so) {

		double dist = Double.MAX_VALUE;

		Vector2d pos = so.getAvatarPosition();
		double posicionJugador[] = new double[2];
		double posicionNPC[] = new double[2];
		posicionJugador[0] = pos.x / 16;
		posicionJugador[1] = pos.y / 16;

		ArrayList<Observation>[] posiciones = so.getNPCPositions(pos);

		if (posiciones[0].size() > 0) {
			Vector2d aux = posiciones[0].get(0).position;
			posicionNPC[0] = aux.x / 16;
			posicionNPC[1] = aux.y / 16;

			double diferenciaX = posicionJugador[0] - posicionNPC[0];
			double diferenciaY = posicionJugador[1] - posicionNPC[1];

			dist = Math.sqrt(Math.pow(diferenciaX, 2) + Math.pow(diferenciaY, 2));

			System.out.println(dist + " Distancia menor");
		} else {
			dist = 1000000000000.0;
		}
		

		return dist;
	}

	public static double[] getPosicionJugadorMod(StateObservation so) {

		double x = -1.0;
		double y = -1.0;
		boolean encontrado = false;
		double posicionJugador[] = new double[2];
		for (int j = 0; j < so.getObservationGrid()[0].length; j++) {
			for (int i = 0; i < so.getObservationGrid().length; i++) {
				if (so.getObservationGrid()[i][j].size() != 0) {
					if (so.getObservationGrid()[i][j].get(0).itype == 1 && !encontrado) {
						x = 2 * (i) + 2 * (i + 1);
						y = 2 * (j) + 2 * (j + 1);
						x = x / 4;
						y = y / 4;
						posicionJugador[0] = x;
						posicionJugador[1] = y;
						encontrado = true;
					}
				}

			}
		}

//		System.out.print(x + " " + y + " Jugador ");
		return posicionJugador;
	}

	public static double[] getPosicionChungo(StateObservation so) {

		double x = -1.0;
		double y = -1.0;
		boolean encontrado = false;
		double posicionJugadorChungo[] = new double[2];
		for (int j = 0; j < so.getObservationGrid()[0].length; j++) {
			for (int i = 0; i < so.getObservationGrid().length; i++) {
				if (so.getObservationGrid()[i][j].size() != 0) {
					if (so.getObservationGrid()[i][j].get(0).itype == 7 && !encontrado) {
						x = 2 * (i) + 2 * (i + 1);
						y = 2 * (j) + 2 * (j + 1);
						x = x / 4;
						y = y / 4;
						posicionJugadorChungo[0] = x;
						posicionJugadorChungo[1] = y;
						encontrado = true;
					}
				}

			}
		}

//		System.out.print(x + " " + y + " Chungo ");
		return posicionJugadorChungo;
	}

	public static double distanciaEuclideaJugadorChungo(StateObservation so) {
		double dist;

		double posJugador[] = getPosicionJugadorMod(so);
		double posChungo[] = getPosicionChungo(so);

		double diferenciaX = posJugador[0] - posChungo[0];
		double diferenciaY = posJugador[1] - posChungo[1];

		dist = Math.sqrt(Math.pow(diferenciaX, 2) + Math.pow(diferenciaY, 2));

		return dist;
	}

	public static int numeroInfectados(StateObservation so) {
		ArrayList<Observation>[] posicionesNPCs = so.getNPCPositions();
		return posicionesNPCs[0].size();
	}

	public static int numeroSanos(StateObservation so) {
		ArrayList<Observation>[] posicionesNPCs = so.getNPCPositions();
		return posicionesNPCs[0].size();
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

	public static void showMapaNuevo(StateObservation so) {

		// itype 0 -> muros
		// itype -> 1 Jugador
		// itype -> 4 personaje infectado
		// itype -> 6 personaje bueno
		// itype -> 7 tio chungo

		int numCol = so.getWorldDimension().width / so.getBlockSize();
		int numFil = so.getWorldDimension().height / so.getBlockSize();

		for (int i = 0; i < numFil; i++) {
			for (int j = 0; j < numCol; j++) {
				if (so.getObservationGrid()[j][i].size() != 0) {

					System.out.print(so.getObservationGrid()[j][i].get(0).itype);
				} else {
					System.out.print('-');
				}

			}
			System.out.println();
		}

	}

}