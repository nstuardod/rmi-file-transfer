package cl.ulagos.icinf.rmi.client;

import java.net.URL;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/*
 * Lanzador de la aplicación cliente.
 */
public class Launcher {
	public static void main(String[] args) {
		// Me aburrí de reinventarlo todo asi que importemos algo.
		Options opts = new Options();
		Option cliMode = new Option("t", true, //$NON-NLS-1$
				StringTable.getString("Launcher.TEXT_PARAMETER_HELP")); //$NON-NLS-1$
		cliMode.setRequired(false);
		cliMode.setArgName("server"); //$NON-NLS-1$
		opts.addOption(cliMode);

		Option help = new Option("h", StringTable.getString("Launcher.HELP_PARAMETER_DESCRIPTION")); //$NON-NLS-1$ //$NON-NLS-2$
		help.setRequired(false);
		opts.addOption(help);

		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd;

		boolean startTextClient = false;
		String textClientHostname = new String();

		try {
			cmd = parser.parse(opts, args);

			if (cmd.hasOption("h")) { //$NON-NLS-1$
				formatter.printHelp(StringTable.getString("Launcher.HELP_COMMAND"), null, opts, StringTable.getString("Launcher.HELP_FOOTER")); //$NON-NLS-1$ //$NON-NLS-2$
				System.exit(0);
			}
			
			if (cmd.hasOption("t")) { //$NON-NLS-1$
				textClientHostname = cmd.getOptionValue("t"); //$NON-NLS-1$
				startTextClient = true;
			}
		} catch (ParseException e) {
			System.out.println(e.getMessage());
			formatter.printHelp(StringTable.getString("Launcher.HELP_COMMAND"), null, opts, StringTable.getString("Launcher.HELP_FOOTER")); //$NON-NLS-1$ //$NON-NLS-2$
			System.exit(1);
		}

		// Preparemos las políticas acá
		String policyPath = "res/client.policy"; //$NON-NLS-1$
		URL policyURL = TextModeClient.class.getClassLoader().getResource(policyPath);
		if (policyURL == null) {
			System.err.println(String.format(StringTable.getString("Launcher.COULD_NOT_LOAD_RESOURCE"),policyPath)); //$NON-NLS-1$
			return;
		} else {
			String url = policyURL.toString();
			System.out.println(String.format(StringTable.getString("Launcher.POLICY_FOUND"), url)); //$NON-NLS-1$
			System.setProperty("java.security.policy", url); //$NON-NLS-1$
		}

		if (System.getSecurityManager() == null)
			System.setSecurityManager(new SecurityManager());

		if (startTextClient) {
			TextModeClient.start(textClientHostname);
		} else {
			new GuiClient().start();
		}
	}

}
