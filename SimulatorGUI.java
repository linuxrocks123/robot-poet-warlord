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
 * should look like a normal program on whatever OS you run this on.<br><br>
 *
 * This is VERY rough at the moment ... I'm testing the logic code, so I
 * only really wrote enough of the GUI frontend to be able to do that.
 */
import java.awt.*;
import java.awt.event.*;
import javax.swing.JOptionPane;
import javax.swing.Timer;
public class SimulatorGUI extends Frame
{
     //Program State
     private RoboSim current_sim;
     private Timer ticker;

     //GUI Components
     private List playerList;
     private Canvas canvas;
     private TextField speed;
     private TextField addPlayerField;
     private Button reset, addPlayer, setSpeed, startstop;
     
     //Parameters for RoboSim
     private int initial_robots_per_combatant;
     private int skill_points;
     private int length;
     private int width;
     private int obstacles;

     public SimulatorGUI(int gridX, int gridY, int skillz, int bots_per_player, int obstacles_) {
          //Store RoboSim parameters
          length=gridX;
          width=gridY;
          skill_points=skillz;
          initial_robots_per_combatant = bots_per_player;
          obstacles=obstacles_;

         //Set up window
         setSize(500,500);
         addWindowListener(new WindowAdapter() {
                   public void windowClosing(WindowEvent we) {
                        dispose();
                   }
              });
         
		GridBagLayout gbl_panel = new GridBagLayout();
		gbl_panel.columnWidths = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 } ;
		gbl_panel.rowHeights = new int[]{0, 0};
		gbl_panel.columnWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		gbl_panel.rowWeights = new double[]{1.0, Double.MIN_VALUE};
		setLayout(gbl_panel);
		
		playerList = new List();
		playerList.setPreferredSize(new Dimension(3, 8));;
		GridBagConstraints gbc_list = new GridBagConstraints();
		gbc_list.fill = GridBagConstraints.BOTH;
		gbc_list.insets = new Insets(5,5,5,5);
		gbc_list.gridwidth = 2;
		gbc_list.gridheight = 5;
		gbc_list.gridx = 0;
		gbc_list.gridy = 0;
		add(playerList, gbc_list);
		
		canvas = new Canvas() {
			public void paint(Graphics g)
			{
                 //Get bounds of rid
                 Rectangle r = getBounds();
                 int cell_length = r.width/length;
                 int cell_height = r.height/width;

                 //Draw grid
                 for(int i=0; i<=length; i++)
                      g.drawLine(0,cell_height*i,cell_length*length,cell_height*i);
                 for(int i=0; i<=width; i++)
                      g.drawLine(cell_length*i,0,cell_length*i,cell_height*width);

                 //If there's no current sim, we need to exit now
                 if(current_sim==null)
                      return;

                 //Generate color mapping
                 HashMap<String,Color> colorMap = new HashMap<String,Color>();
                 Color current_color = Color.BLACK;
                 for(String x : playerList.getItems())
                 {
                      //Update color
                      if(current_color.equals(Color.BLACK))
                           current_color = Color.RED;
                      else if(current_color.equals(Color.RED))
                           current_color = Color.PINK;
                      else if(current_color.equals(Color.PINK))
                           current_color = Color.ORANGE;
                      else if(current_color.equals(Color.ORANGE))
                           current_color = Color.YELLOW;
                      else if(current_color.equals(Color.YELLOW))
                           current_color = Color.GREEN;
                      else if(current_color.equals(Color.GREEN))
                           current_color = Color.MAGENTA;
                      else if(current_color.equals(Color.MAGENTA))
                           current_color = Color.CYAN;
                      else if(current_color.equals(Color.CYAN))
                           current_color = Color.BLUE;
                      else if(current_color.equals(Color.BLUE))
                           current_color = Color.LIGHT_GRAY;
                      else if(current_color.equals(Color.LIGHT_GRAY))
                           current_color = Color.GRAY;
                      else if(current_color.equals(Color.GRAY))
                           current_color = Color.DARK_GRAY;
                      else if(current_color.equals(Color.DARK_GRAY))
                           current_color = Color.PINK;

                      //Add mapping
                      colorMap.put(x,current_color);
                 }

                 //Iterate over RoboSim grid and "paint" squares that need to be painted
                 Robot.GridCell[][] world = current_sim.getWorldGrid();
                 for(int i=0; i<length; i++)
                      for(int j=0; j<width; j++)
                           switch(world[i][j].contents)
                           {
                           case BLOCKED:
                                //Draw a black 'X' in the cell
                                g.setColor(Color.BLACK);
                                g.drawLine(i*cell_length,j*cell_height,(i+1)*cell_length,(j+1)*cell_height);
                                g.drawLine(i*cell_length,(j+1)*cell_height,(i+1)*cell_length,j*cell_height);
                                break;
                           case SELF:
                                //Fill rectangle with color representing player
                                g.setColor(colorMap.get(current_sim.getOccupantPlayer(world[i][j])));
                                g.fillRect(i*cell_length,j*cell_height,cell_length,cell_height);
                                break;
                           case WALL: //TODO
                           case FORT: //TODO
                           case CAPSULE: //TODO
                 
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
		
		speed = new TextField();
		GridBagConstraints gbc_speed = new GridBagConstraints();
		gbc_speed.insets = new Insets(0,0,0,5);
		gbc_speed.fill = GridBagConstraints.BOTH;
		gbc_speed.gridwidth = 1;
		gbc_speed.gridheight = 1;
		gbc_speed.gridx = 2;
		gbc_speed.gridy = 5;
		add(speed,gbc_speed);

		addPlayerField = new TextField();
		addPlayerField.setBounds(0,0,50,50);
		addPlayerField.setPreferredSize(new Dimension(20,20));
		GridBagConstraints gbc_field = new GridBagConstraints();
		gbc_field.fill = GridBagConstraints.BOTH;
		gbc_field.insets = new Insets(0,5,0,5);
		gbc_field.gridwidth = 2;
		gbc_field.gridheight = 1;
		gbc_field.gridx = 0;
		gbc_field.gridy = 5;
		add(addPlayerField,gbc_field);
		
		reset = new Button("Reset");
		GridBagConstraints gbc_reset = new GridBagConstraints();
		gbc_reset.fill = GridBagConstraints.BOTH;
		gbc_reset.gridx = 0;
		gbc_reset.gridy = 6;
		add(reset,gbc_reset);

        //Reset button destroys everything
        reset.addActionListener(new ActionListener() {
                  public void actionPerformed(ActionEvent e)
                       {
                            current_sim = null;
                            ticker.stop();
                            playerList.removeAll();
                            canvas.repaint();
                       }
             });
		
		addPlayer = new Button("Add Player");
		GridBagConstraints gbc_player = new GridBagConstraints();
		gbc_player.fill = GridBagConstraints.BOTH;
		gbc_player.gridx = 1;
		gbc_player.gridy = 6;
		add(addPlayer,gbc_player);
		
        //Add Player button adds a player to the list
        addPlayer.addActionListener(new ActionListener() {
                  public void actionPerformed(ActionEvent e)
                       {
                            if(!addPlayerField.getText().equals(""))
                                 playerList.add(addPlayerField.getText());
                       }
             });

		setSpeed = new Button("Speed");
		setSpeed.setBounds(0,0,10,10);
		GridBagConstraints gbc_speedlabel = new GridBagConstraints();
		gbc_speedlabel.fill = GridBagConstraints.BOTH;
		gbc_speedlabel.gridwidth = 1;
		gbc_speedlabel.gridheight = 1;
		gbc_speedlabel.gridx = 2;
		gbc_speedlabel.gridy = 6;
		add(setSpeed,gbc_speedlabel);

        //Speed sets speed to new value
        setSpeed.addActionListener(new ActionListener() {
                  public void actionPerformed(ActionEvent e)
                       {
                            int newSpeed = 0;
                            try
                            {
                                 newSpeed = Integer.parseInt(speed.getText());
                            }
                            catch(NumberFormatException ne)
                            {
                                 JOptionPane.showMessageDialog(null,"Invalid speed (not an integer)");
                                 return;
                            }

                            if(newSpeed <= 0)
                            {
                                 JOptionPane.showMessageDialog(null,"Invalid speed (negative number)");
                                 return;
                            }

                            boolean wasRunning = ticker.isRunning();
                            if(wasRunning)
                                 ticker.stop();
                            ticker.setInitialDelay(newSpeed);
                            ticker.setDelay(newSpeed);
                            if(wasRunning)
                                 ticker.restart();
                       }
             });

		startstop = new Button("Play");
		GridBagConstraints gbc_startstop = new GridBagConstraints();
		gbc_startstop.fill = GridBagConstraints.BOTH;
		gbc_startstop.gridx = 2;
		gbc_startstop.gridy = 0;
		add(startstop,gbc_startstop);

        //Play button starts world by starting timer
        startstop.addActionListener(new ActionListener() {
                  public void actionPerformed(ActionEvent e)
                       {
                            if(!ticker.isRunning())
                            {
                                 if(current_sim==null)
                                      try
                                      {
                                           current_sim = new RoboSim(playerList.getItems(),initial_robots_per_combatant,skill_points,length,width,obstacles);
                                      }
                                      catch(RoboSim.RoboSimExecutionException m)
                                      {
                                           JOptionPane.showMessageDialog(null,m.getMessage());
                                      }
                                 ticker.start();
                                 startstop.setLabel("Pause");
                            }
                            else
                            {
                                 ticker.stop();
                                 startstop.setLabel("Play");
                            }
                       }
             });

        //Set up timer
        ticker = new Timer(999999, new ActionListener() {
                  public void actionPerformed(ActionEvent e)
                       {
                            String ret = null;
                            try
                            {
                                 ret = current_sim.executeSingleTimeStep();
                            }
                            catch(RoboSim.RoboSimExecutionException m)
                            {
                                 JOptionPane.showMessageDialog(null,m.getMessage());
                                 ret=null;
                                 ticker.stop();
                            }
                                 if(ret!=null)
                                 {
                                      ticker.stop();
                                      JOptionPane.showMessageDialog(null,"The winner is: "+ret);
                                 }
                                 canvas.repaint();
                       }
             });
	}
	
	public static void main(String[] args)
	{
         int x=20, y=20, skill_points=20, bots_per_player=5, obstacles=30;
         if(args.length==5)
         {
              x=Integer.parseInt(args[0]);
              y=Integer.parseInt(args[1]);
              skill_points=Integer.parseInt(args[2]);
              bots_per_player=Integer.parseInt(args[3]);
              obstacles=Integer.parseInt(args[4]);
         }

         SimulatorGUI gui = new SimulatorGUI(x,y,skill_points,bots_per_player,obstacles);
         gui.setVisible(true);
	}
}
