package protoevo.core;

import javax.swing.SwingUtilities;

import protoevo.utils.REPL;
import protoevo.utils.TextStyle;
import protoevo.utils.Window;

public class Application 
{
	public static Simulation simulation;
	public static Window window;
	
	public static final float refreshDelay = 1000 / 120f;
	
	public static void main(String[] args)
	{
//		simulation = new Simulation(Settings.simulationSeed, "pontus-seel-officiis");
		simulation = new Simulation(Settings.simulationSeed, "pontus-parasect-ipsam");

		try {
			if (!(args.length > 0 && args[0].equals("noui"))) {
				TextStyle.loadFonts();
				window = new Window("Evolving Protozoa", simulation);
				SwingUtilities.invokeLater(window);
			}
			else {
				simulation.setUpdateDelay(0);
			}
			new Thread(new REPL(simulation, window)).start();
			simulation.simulate();
		}
		catch (Exception e) {
			simulation.close();
			throw e;
		}
	}
	
	public static void exit()
	{
		System.exit(0);
	}
}