package qlearning;

import java.awt.Dimension;
import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import ontology.Types.ACTIONS;
import qlearning.StateManager.ESTADOS;

import tools.ElapsedCpuTimer;

public class TrainingAgent extends AbstractPlayer {
	boolean verbose = StateManager.verbose;

	private double alpha = 0.7; // Factor Exploracion
	private double gamma = 0.2; // Factor descuento

	boolean randomPolicy = false; // RandomPolicy o MaxQ

	/* Variables */
	ArrayList<Observation>[] inmov;
	Dimension dim;
	private int numFilas;
	private int numCol;
	private char[][] mapaObstaculos;

	/* Variables Q-Learning */
	static int numAccionesPosibles;
	protected Random randomGenerator; // Random generator for the agent
	protected ArrayList<Types.ACTIONS> actions; // List of available actions for the agent

	/**
	 * Public constructor with state observation and time due.
	 * 
	 * @param so           state observation of the current game.
	 * @param elapsedTimer Timer for the controller creation. En el constructor
	 *                     mirar y guardar las cosas estaticas
	 */
	public TrainingAgent(StateObservation so, ElapsedCpuTimer elapsedTimer) {
		if (verbose)
			System.out.println("______________________\nCOMIENZA LA PARTIDA\n_______________________");
		randomGenerator = new Random();
		actions = so.getAvailableActions();
		inmov = so.getImmovablePositions();
		dim = so.getWorldDimension();

		so.getBlockSize();

		numCol = so.getWorldDimension().width / so.getBlockSize();
		numFilas = so.getWorldDimension().height / so.getBlockSize();

		if (verbose)
			System.out.println("DIMENSION MUNDO: " + so.getWorldDimension().toString());
		if (verbose)
			System.out.println("NUM FILAS = " + numFilas);
		if (verbose)
			System.out.println("NUM COL = " + numCol);

		// Inicializamos el modulo Util
		StateManager.numCol = this.numCol;
		StateManager.numFilas = this.numFilas;

		numAccionesPosibles = StateManager.ACCIONES.length;

	}

	/**
	 * Picks an action. This function is called every game step to request an action
	 * from the player.
	 * 
	 * @param stateObs     Observation of the current state.
	 * @param elapsedTimer Timer when the action returned is due.
	 * @return An action for the current state
	 */
	public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
		// -----------------------------------------------------------------------
		// 01 - PERCEPCI�N DEL ENTORNO
		// -----------------------------------------------------------------------

		int distanciaJP = StateManager.distanciaMeta(stateObs);
		
	
		
		this.mapaObstaculos = StateManager.getMapa(stateObs); // Actualizamos el mapa percibido

		//if (verbose)			StateManager.showMapa(stateObs);

		// Percibimos el estado actual e incrementamos su contador
		ESTADOS estadoActual = StateManager.getEstado(stateObs, mapaObstaculos,distanciaJP);
		estadoActual.incrementa();
		if (verbose)
			System.out.println("Estado actual: " + estadoActual.toString());

		// -----------------------------------------------------------------------
		// ALGORITMO Q LEARNING
		// -----------------------------------------------------------------------

		if (verbose)
			StateManager.pintaQTable(estadoActual);

		// Seleccionar una entre las posibles acciones desde el estado actual
		ACTIONS action;

		// Criterio de selecci�n: random hasta 1/3 iteraciones
		if (StateManager.iteracionActual < StateManager.numIteraciones * 0.3)
			randomPolicy = true;
		else
			randomPolicy = false;

		// Criterio de selecci�n: random
		if (randomPolicy) {

			int index = randomGenerator.nextInt(numAccionesPosibles);
			action = StateManager.ACCIONES[index];
		} else // Criterio seleccion: maxQ
		{
			action = StateManager.getAccionMaxQ(estadoActual);
		}

		if (verbose)
			System.out.println("--> DECIDE HACER: " + action.toString());

		// Calcular el siguiente estado habiendo elegido esa acci�n
		ESTADOS estadoSiguiente = StateManager.getEstadoFuturo(stateObs, action,distanciaJP);
		if (verbose)
			System.out.println("ESTADO SIGUIENTE: " + estadoSiguiente.toString());

		// Using this possible action, consider to go to the next state
		double q = StateManager.Q.get(new ParEstadoAccion(estadoActual, action));
		
		
		if (verbose)
			System.out.println("Consulto q actual Q<" + estadoActual.toString() + "," + action.toString() + "> = " + q);

//		double maxQ = maxQ(estadoSiguiente);
		double maxQ = maxQ(estadoActual);

		double r = StateManager.R.get(new ParEstadoAccion(estadoActual, action));
//        double r = StateManager.R.get(new ParEstadoAccion(estadoSiguiente, action));
		double value = q + alpha * (r + gamma * maxQ - q);
		// Actualizamos la tabla Q
		StateManager.Q.put(new ParEstadoAccion(estadoActual, action), value);

		if (verbose)
			System.out.println("--> DECIDE HACER: " + action.toString());

		// if(stateObs.isGameOver()) this.saveQTable(); //Guardamos la tablaQ si termina




		return action;
	}

	private double maxQ(ESTADOS s) {
		ACTIONS[] actions = StateManager.ACCIONES;
		double maxValue = Double.MIN_VALUE;

		for (int i = 0; i < actions.length; i++) {

			// if(verbose) System.out.print("maxQ<"+ s.toString() + "," );
			// if(verbose) System.out.print(actions[i]+"> = ");
			double value = StateManager.Q.get(new ParEstadoAccion(s, actions[i]));
			// if(verbose) System.out.println(value);

			if (value > maxValue)
				maxValue = value;
		}

		return maxValue;
	}
}