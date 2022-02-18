package ql;

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
import ql.Controlador.ESTADOS;
import tools.ElapsedCpuTimer;

public class AgenteEntrenamiento extends AbstractPlayer {

	private double alpha = 0.1; // 0.7 Factor Exploracion
	private double gamma = 0.2; // Factor descuento

	boolean tipoPolitica = false; // Si es true Random

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
	public AgenteEntrenamiento(StateObservation so, ElapsedCpuTimer elapsedTimer) {

		randomGenerator = new Random();
		actions = so.getAvailableActions();
		inmov = so.getImmovablePositions();
		dim = so.getWorldDimension();

		so.getBlockSize();

		numCol = so.getWorldDimension().width / so.getBlockSize();
		numFilas = so.getWorldDimension().height / so.getBlockSize();

		numAccionesPosibles = Controlador.ACCIONES.length;

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
		// 01 - PERCEPCIÓN DEL ENTORNO
		// -----------------------------------------------------------------------

		

		// Estado actual
		int infectados = Controlador.numeroInfectados(stateObs);
		double jugadorChungo = Controlador.distanciaEuclideaJugadorChungo(stateObs);
		double jugadorInfectadoCercano = Controlador.getDistanciaJugadorNPCCercano(stateObs);
		
		ESTADOS estadoActual = Controlador.getEstado(stateObs,Controlador.getMapa(stateObs), infectados,jugadorChungo,jugadorInfectadoCercano);
		estadoActual.incrementa();
		System.out.println("Estado actual: " + estadoActual.toString());

		// Controlador.pintaQTable(estadoActual);

		ACTIONS action;

		// Al inicio hasta el 30% cogemos acciones random, cuando supere usamos el valor
		// maximo en la tablaQ para el estado
		if (Controlador.partidaActual < Controlador.numPartidasEntrenamiento * 0.3)
			tipoPolitica = true;
		else
			tipoPolitica = false;

		if (tipoPolitica) {

			int index = randomGenerator.nextInt(numAccionesPosibles);
			action = Controlador.ACCIONES[index];
		} else

		{
			// Usamos la tabla
			action = Controlador.getAccionMaxQ(estadoActual);
		}
//
		System.out.println(" Accion elegida: " + action.toString());

		// Calcular el estado siguiente
		ESTADOS estadoSiguiente = Controlador.getEstadoFuturo(stateObs, action, infectados,jugadorChungo,jugadorInfectadoCercano);
	
		System.out.println("Proximo estado " + estadoSiguiente.toString());

		double q = Controlador.Q.get(new EstadoAccion(estadoActual, action));

		System.out.println("Valor actual q <" + estadoActual.toString() + "," + action.toString() + "> = " + q);

//		double maxQ = maxQ(estadoSiguiente);
		double maxQ = maxQ(estadoActual);
//		double r = StateManager.R.get(new EstadoAccion(estadoActual, action));
		double r = Controlador.R.get(new EstadoAccion(estadoSiguiente, action));
		double value = q + alpha * (r + gamma * maxQ - q);

		// Se modifica el valor de la tablaQ
		Controlador.Q.put(new EstadoAccion(estadoActual, action), value);

		System.out.println("Accion elegida: " + action.toString());

		// if(stateObs.isGameOver()) this.saveQTable(); //Guardamos la tablaQ si termina

		return action;
	}

	private double maxQ(ESTADOS s) {
		ACTIONS[] actions = Controlador.ACCIONES;
		double maxValue = Double.MIN_VALUE;

		for (int i = 0; i < actions.length; i++) {

			double value = Controlador.Q.get(new EstadoAccion(s, actions[i]));

			if (value > maxValue)
				maxValue = value;
		}

		return maxValue;
	}
}