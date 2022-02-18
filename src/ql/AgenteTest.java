package ql;

import java.util.ArrayList;
import java.util.Random;

import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import ontology.Types.ACTIONS;
import ql.Controlador.ESTADOS;
import tools.ElapsedCpuTimer;

public class AgenteTest extends AbstractPlayer {

	int numAccionesPosibles;

	protected Random randomGenerator;
	protected ArrayList<Types.ACTIONS> actions;

	/**
	 * Public constructor with state observation and time due.
	 * 
	 * @param so           state observation of the current game.
	 * @param elapsedTimer Timer for the controller creation. En el constructor
	 *                     mirar y guardar las cosas estaticas
	 */
	public AgenteTest(StateObservation so, ElapsedCpuTimer elapsedTimer) {
		randomGenerator = new Random();
		actions = so.getAvailableActions();

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

		int infectados = Controlador.numeroInfectados(stateObs);
		double jugadorChungo = Controlador.distanciaEuclideaJugadorChungo(stateObs);
		double jugadorInfectadoCercano = Controlador.getDistanciaJugadorNPCCercano(stateObs);

		ESTADOS estadoActual = Controlador.getEstado(stateObs,Controlador.getMapa(stateObs), infectados,jugadorChungo,jugadorInfectadoCercano);
		estadoActual.incrementa();
		System.out.println("Estado actual: " + estadoActual.toString());

		Controlador.pintaQTable(estadoActual);

		ACTIONS action = Controlador.getAccionMaxQ(estadoActual);

		System.out.println(" Accion elegida: " + action.toString());

		return action;

	}
}
