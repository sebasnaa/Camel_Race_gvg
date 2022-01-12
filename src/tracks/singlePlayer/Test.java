package tracks.singlePlayer;

import java.util.Random;

import qlearning.StateManager;
import tools.Utils;
import tracks.ArcadeMachine;

public class Test {

	public static void main(String[] args) {

		String QLearningTraining = "qlearning.TrainingAgent";
		String QLearningTesting = "qlearning.TestingAgent";

		// Load available games
		String spGamesCollection = "examples/all_games_sp.csv";
		String[][] games = Utils.readGames(spGamesCollection);

		// Game settings
		boolean visuals = true;
		int seed = new Random().nextInt();

		// Game and level to play
		int gameIdx = 15;

		String gameName = games[gameIdx][1];
		String game = games[gameIdx][0];

		String recordActionsFile = null;// "actions_" + games[gameIdx] + "_lvl"

		int levelIdx = 5; // level names from 0 to 4 (game_lvlN.txt).
		String level1 = game.replace(gameName, gameName + "_lvl" + levelIdx);
		StateManager stateManager;

		boolean training = false; // Modo entrenamiento, crea una nueva tabla Q y juega M partidas aleatorias
		boolean verbose = false; // Mostrar informacion de la partida mientras se ejecuta
		boolean probarNiveles = false;

		if (probarNiveles) // Probar todos los niveles
		{
			visuals = true;
			double[] ticksPartidas = new double[7];

			stateManager = new StateManager("TablaQ.csv", verbose);
			for (int i = 0; i <=6; i++) {

				levelIdx = i; // level names from 0 to 4 (game_lvlN.txt).
				level1 = game.replace(gameName, gameName + "_lvl" + levelIdx);
				// QLearningTesting QLearningTraining
				ticksPartidas[i] = ArcadeMachine.runOneGame(game, level1, visuals, QLearningTesting, recordActionsFile,
						seed, 0)[2];
			}

		}

		if (training) // Crea la tabla Q a random y juega partidas con acciones aleatorias
		{
			visuals = false;
			boolean testingAfterTraining = false; // Probar todos los niveles despues del entrenamiento
			boolean randomTablaQ = true; // Verdadero: crea la tabla Q con valores random, si no, a cero
			stateManager = new StateManager(randomTablaQ, false);
			StateManager.numIteraciones = 100; // Numero de partidas a jugar

			for (StateManager.iteracionActual = 1; StateManager.iteracionActual <= StateManager.numIteraciones; StateManager.iteracionActual++) {
				levelIdx = new Random().nextInt(7); // 5 // level names from 0 to 4 (game_lvlN.txt).
				level1 = game.replace(gameName, gameName + "_lvl" + levelIdx);
				System.out.println("\t\t\t\t\t\t\t\t\t\tIteración " + StateManager.iteracionActual + " / "
						+ StateManager.numIteraciones);
				System.out.println("\t\t\t\t\t\t\t\t\t\tlevel: " + levelIdx);
				ArcadeMachine.runOneGame(game, level1, visuals, QLearningTraining, recordActionsFile, seed, 0);

			}

			stateManager.saveQTable();

			if (testingAfterTraining) // Probar todos los niveles
			{
				visuals = true;
				double[] ticksPartidas = new double[7];

				stateManager = new StateManager("TablaQ.csv", verbose);
				for (int i = 0; i <= 4; i++) {

					levelIdx = i; // level names from 0 to 4 (game_lvlN.txt).
					level1 = game.replace(gameName, gameName + "_lvl" + levelIdx);
					// QLearningTesting QLearningTraining
					ticksPartidas[i] = ArcadeMachine.runOneGame(game, level1, visuals, QLearningTesting,
							recordActionsFile, seed, 0)[2];
				}

			}
		} else // Test
		{
			stateManager = new StateManager("TablaQ.csv", false);
			ArcadeMachine.runOneGame(game, level1, visuals, QLearningTesting, recordActionsFile, seed, 0);
		}

//		stateManager.getContadoresEstados();
//
//		StateManager.pintaQTableResumen();

	}
}
