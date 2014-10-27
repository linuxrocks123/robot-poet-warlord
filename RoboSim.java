/**
 * RoboSim: Main simulator logic class.
 */
import java.util.Random;
import java.lang.reflect.Constructor;
public class RoboSim
{
     public static class RoboSimCreationException extends Exception
     {
          public String player;
          public RoboSimCreationException(String message, String player_)
               {
                    super(message);
                    player = player_;
               }
     }

     private static class RobotData
     {
          public int x_pos;
          public int y_pos;
          public Robot.Robot_Specs specs;
          public Robot.Robot_Status status;
          public Robot robot;
          public String player;
     }

     private static class SimGridCell extends Robot.GridCell
     {
          public RobotData occupant_data;
     }

     private SimGrid[][] worldGrid;
     private RobotData[] turnOrder;
     private SimulatorGUI gui;

     private static Robot_Specs checkSpecsValid(Robot_Specs proposed, String player, int skill_points) throws RoboSimCreationException
          {
               if(proposed.attack + proposed.defense + proposed.power + proposed.charge != skill_points)
                    throw new RoboSimCreationException("Player "+player+" attempted to create invalid robot!",player);
               return proposed;
          }

     /**
      * Constructor for RoboSim:
      * @param combatants array of String objects containing the names of
      *                   the Robot classes for each combatant
      * @param initial_robots_per_combatant how many robots each team starts
      *                                     out with
      * @param skill_points skill points per combatant
      * @param length length of arena
      * @param width width of arena
      */
    public RoboSim(String[] combatants, int initial_robots_per_combatant, int skill_points, int length, int width) throws RoboSimCreationException
          {
               //Create grid
               for(int i=0; i<length; i++)
               {
                    worldGrid[i] = new SimGridCell[width];
                    for(int j=0; j<width; j++)
                    {
                         worldGrid[i][j] = new SimGridCell();
                         worldGrid[i][j].x_coord = i;
                         worldGrid[i][j].y_coord = j;
                         worldGrid[i][j].contents = SimGridCell.GridObject.EMPTY;
                    }
               }

               //Random number generator
               Random generator = new Random();

               //Initialize array to hold turn order
               turnOrder = new RobotData[combatants.length*initial_robots_per_combatant];

               //Position in turnOrder
               turnOrder_pos = 0;

               //Add robots for each combatant
               for(String player : combatants)
               {
                    Class playerRobotClass = Class.forName(player);
                    Constructor<Robot> gen_robot = playerRobotClass.getConstructor();
                    for(int i=0; i<initial_robots_per_combatant; i++)
                    {
                         int x_pos,y_pos;
                         do
                         {
                              x_pos = generator.nextInt(length);
                              y_pos = generator.nextInt(width);
                         } while(worldGrid[x_pos][y_pos].contents!=SimGridCell.GridObject.EMPTY);

                         worldGrid[x_pos][y_pos].contents = SimGridCell.GridObject.SELF;
                         RobotData data = worldGrid[x_pos][y_pos].occupant_data = new RobotData();
                         data.x_pos = x_pos;
                         data.y_pos = y_pos;
                         data.robot = gen_robot.newInstance();
                         data.player = player;
                         byte[] creation_message = new byte[64];
                         byte[1] = turnOrder_pos % 256;
                         byte[0] = turnOrder_pos / 256;
                         data.specs = checkSpecsValid(data.robot.createRobot(null, skill_points, creation_message), player, skill_points);
}
