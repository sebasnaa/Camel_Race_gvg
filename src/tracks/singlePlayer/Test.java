package tracks.singlePlayer;


import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import ql.ImagenGrafico;
import ql.Controlador;
import tools.Utils;
import tracks.ArcadeMachine;

public class Test {

	public static void main(String[] args) {

		String agenteEntrenamiento = "ql.AgenteEntrenamiento";
		String agenteTest = "ql.AgenteTest";

		// Load available games
		String spGamesCollection = "examples/all_games_sp.csv";
		String[][] games = Utils.readGames(spGamesCollection);

		// Game settings
		boolean visuals = true;
		int seed = new Random().nextInt();

		// Game and level to play
		int gameIdx = 3;

		String gameName = games[gameIdx][1];
		String game = games[gameIdx][0];

		String recordActionsFile = null;// "actions_" + games[gameIdx] + "_lvl"

		int levelIdx = 0; // level names from 0 to 4 (game_lvlN.txt).
		String level1 = game.replace(gameName, gameName + "_lvl" + levelIdx);
		
//		ArcadeMachine.playOneGame(game, level1, recordActionsFile, seed);
		

		
		Controlador controlador;
//
		boolean training = true; // Modo entrenamiento, crea una nueva tabla Q y juega M partidas aleatorias
		boolean probarNiveles = false;
		boolean probarNivel = false;
		boolean grafica = false;

		if (training) // Crea la tabla Q a random y juega partidas con acciones aleatorias y sobre la
						// tablaQ
		{
			visuals = true;
			boolean isTablaQRandom = true;
			controlador = new Controlador(isTablaQRandom);
			Controlador.numPartidasEntrenamiento = 1;

			double[] Y = null;
			double[] X = null;
			ImagenGrafico graficaT = null;

			if (grafica) {
				graficaT = new ImagenGrafico();

				X = new double[Controlador.numPartidasEntrenamiento]; // Epoca
				Y = new double[Controlador.numPartidasEntrenamiento]; // Resultado Ticks

				for (int i = 0; i < X.length; i++) {
					X[i] = i;
				}
			}

			for (Controlador.partidaActual = 1; Controlador.partidaActual <= Controlador.numPartidasEntrenamiento; Controlador.partidaActual++) {
				levelIdx = new Random().nextInt(5);
				level1 = game.replace(gameName, gameName + "_lvl" + levelIdx);
				System.out.println("\t\t\t\t\t\t\t\t\t\tIteración " + Controlador.partidaActual + " / "
						+ Controlador.numPartidasEntrenamiento);
				System.out.println("\t\t\t\t\t\t\t\t\t\tlevel: " + levelIdx);
				double ticks = ArcadeMachine.runOneGame(game, level1, visuals, agenteEntrenamiento, recordActionsFile,
						seed, 0)[2];
				if (grafica) {
					Y[Controlador.partidaActual - 1] = ticks;
				}
			}

			controlador.saveQTable();

			// Creamos grafica segun los ticks necesarios para acabar el juego
			if (grafica) {
				String fecha = java.time.LocalDate.now().toString();
				String ficheroT = fecha + "_PoliticaRandom.jpeg";

				graficaT.plot(X, Y, "-r", 2.0f, "TICKS");
				graficaT.RenderPlot();
				graficaT.title("Resultado partida en Ticks / Epoca de Training");
				graficaT.xlim(1, Controlador.numPartidasEntrenamiento);
				graficaT.ylim(1, 550);
				graficaT.xlabel("Epoca de Training");
				graficaT.ylabel("Resultado Ticks partida");
				graficaT.saveas(ficheroT, 640, 480);

				File file = new File(ficheroT);
				try {
					Desktop.getDesktop().open(file);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

		}

		if (probarNivel) {
			controlador = new Controlador("TablaQT.csv");
			ArcadeMachine.runOneGame(game, level1, true, agenteTest, recordActionsFile, seed, 0);
		}

		if (probarNiveles) // Probar todos los niveles
		{
			visuals = true;
			double[] ticksPartidas = new double[7];

			controlador = new Controlador("TablaQT.csv");
			for (int i = 0; i < 5; i++) {

				levelIdx = i; // level names from 0 to 4 (game_lvlN.txt).
				level1 = game.replace(gameName, gameName + "_lvl" + levelIdx);
				ticksPartidas[i] = ArcadeMachine.runOneGame(game, level1, true, agenteTest, recordActionsFile, seed,
						0)[2];
			}

		}

//		stateManager.getContadoresEstados();
		Controlador.pintaQTableResumen();
//
	}
}
