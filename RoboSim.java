/**
 * RoboSim: Main simulator logic class.
 */
import java.util.Random;
import java.lang.reflect.Constructor;
public class RoboSim
{
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

     /**
      * Constructor for RoboSim:
      * @param combatants array of String objects containing the names of
      *                   the Robot classes for each combatant
      * @param initial_robots_per_combatant how many robots each team starts
      *                                     out with
      * @param length length of arena
      * @param width width of arena
      */
     public RoboSim(String[] combatants, int initial_robots_per_combatant, int length, int width)
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
                         data.specs = robot.createRobot(
}
