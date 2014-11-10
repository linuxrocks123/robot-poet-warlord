/**
 * SimulatorGUI: Main GUI class.<br><br>
 * I was sorely tempted to use ASCII art here.  REALLY, sorely tempted...
 * But I did it right and used AWT and Swing Timers and and a Canvas.
 * The robots are little colored squares.  Got to draw the line somewhere.
 * <br><br>RANT follows:<br>
 * Java GUIs are severely broken because the language can't make up its mind
 * on what it wants programmers to use for a GUI toolkit and has gone from
 * AWT to Swing to JavaFX 1 to JavaFX 2 to JavaFX 8 in the course of its
 * 20-year history.  That may sound like a long time but QT has been around
 * longer and has provided much better continuity.  And this isn't even
 * counting non-Sun efforts like SWT which were done by other parties
 * specifically because of the inadequacy of Java UI development toolkits
 * for substantial use cases.<br><br>
 * I used AWT even though it's "obsolete" as Swing has a tendency to look
 * very non-native.  This is because Swing internally uses an AWT canvas for
 * everything and "paints" the buttons and other controls on it, instead of
 * using the OS to do this.  AWT instead uses the native OS controls, so it
 * should look like a normal program on whatever OS you run this on.
 */
import java.awt.*;
import java.awt.event.*;
public class SimulatorGUI extends Frame
{
	public SimulatorGUI() {
		GridBagLayout gbl_panel = new GridBagLayout();
		gbl_panel.columnWidths = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 } ;
		gbl_panel.rowHeights = new int[]{0, 0};
		gbl_panel.columnWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		gbl_panel.rowWeights = new double[]{1.0, Double.MIN_VALUE};
		setLayout(gbl_panel);
		
		List list = new List();
		list.setPreferredSize(new Dimension(3, 8));;
		GridBagConstraints gbc_list = new GridBagConstraints();
		gbc_list.fill = GridBagConstraints.BOTH;
		gbc_list.insets = new Insets(5,5,5,5);
		gbc_list.gridwidth = 2;
		gbc_list.gridheight = 5;
		gbc_list.gridx = 0;
		gbc_list.gridy = 0;
		add(list, gbc_list);
		
		Canvas canvas = new Canvas() {
			public void paint(Graphics g)
			{
				Rectangle r = getBounds();
				g.drawRect(1, 1, 30, r.height/2);
			
			}
		};
		canvas.setBounds(0, 0, 50, 50);
		canvas.setBackground(Color.WHITE);
		GridBagConstraints gbc_canvas = new GridBagConstraints();
		gbc_canvas.fill = GridBagConstraints.BOTH;
		gbc_canvas.gridwidth = 9;
		gbc_canvas.gridheight = 7;
		gbc_canvas.gridx = 3;
		gbc_canvas.gridy = 0;
		add(canvas, gbc_canvas);
		
		TextField speed = new TextField();
		GridBagConstraints gbc_speed = new GridBagConstraints();
		gbc_speed.insets = new Insets(0,0,0,5);
		gbc_speed.fill = GridBagConstraints.BOTH;
		gbc_speed.gridwidth = 1;
		gbc_speed.gridheight = 1;
		gbc_speed.gridx = 2;
		gbc_speed.gridy = 5;
		add(speed,gbc_speed);
		
		TextField field = new TextField();
		field.setBounds(0,0,50,50);
		field.setPreferredSize(new Dimension(20,20));
		GridBagConstraints gbc_field = new GridBagConstraints();
		gbc_field.fill = GridBagConstraints.BOTH;
		gbc_field.insets = new Insets(0,5,0,5);
		gbc_field.gridwidth = 2;
		gbc_field.gridheight = 1;
		gbc_field.gridx = 0;
		gbc_field.gridy = 5;
		add(field,gbc_field);
		
		Button reset = new Button("Reset");
		GridBagConstraints gbc_reset = new GridBagConstraints();
		gbc_reset.fill = GridBagConstraints.BOTH;
		gbc_reset.gridx = 0;
		gbc_reset.gridy = 6;
		add(reset,gbc_reset);
		
		Button addplayer = new Button("Add Player");
		GridBagConstraints gbc_player = new GridBagConstraints();
		gbc_player.fill = GridBagConstraints.BOTH;
		gbc_player.gridx = 1;
		gbc_player.gridy = 6;
		add(addplayer,gbc_player);
		
		Button speedlabel = new Button("Speed");
		speedlabel.setBounds(0,0,10,10);
		GridBagConstraints gbc_speedlabel = new GridBagConstraints();
		gbc_speedlabel.fill = GridBagConstraints.BOTH;
		gbc_speedlabel.gridwidth = 1;
		gbc_speedlabel.gridheight = 1;
		gbc_speedlabel.gridx = 2;
		gbc_speedlabel.gridy = 6;
		add(speedlabel,gbc_speedlabel);

		Button startstop = new Button("Play");
		GridBagConstraints gbc_startstop = new GridBagConstraints();
		gbc_startstop.fill = GridBagConstraints.BOTH;
		gbc_startstop.gridx = 2;
		gbc_startstop.gridy = 0;
		add(startstop,gbc_startstop);
	}
	
	public static void main(String[] args)
	{
		SimulatorGUI gui = new SimulatorGUI();
		gui.setVisible(true);
	}
}