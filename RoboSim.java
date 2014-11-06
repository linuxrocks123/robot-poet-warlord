/**
 * RoboSim: Main simulator logic class.
 */
import java.util.ArrayList; //1301 students: it's an array, you just use "get(i)" instead of "[i]"
import java.util.Random;
import java.lang.reflect.Constructor;
public class RoboSim
{
     /**Exception class*/
     public static class RoboSimExecutionException extends Exception
     {
          public String player;

          public RoboSimExecutionException(String message, String player)
               {
                    this(message,player,-1,-1,-1,-1);
               }

          public RoboSimExecutionException(String message, String player, Robot.GridCell cell)
               {
                    this(message,player,cell.x_coord,cell.y_coord,-1,-1);
               }

          public RoboSimExecutionException(String message, String player, Robot.GridCell cell, Robot.GridCell cell2)
               {
                    this(message,player,cell.x_coord,cell.y_coord,cell2.x_coord,cell2.y_coord);
               }

          public RoboSimExecutionException(String message, String player, int x1, int y1, int x2, int y2)
               {
                    super("Player "+player_+" "+message+(x1!=-1 ? " with robot at coordinates ["+x1+"]["+y1+"]" ? "")+(x2!=-1 ? ", coordinates of invalid cell are ["+x2+"]["+y2+"]"));
                    player = player_;
               }
     }

     /**FSPPredicate derivative making use of SimGridCell-specific information*/
     public static class SimGridAllyDeterminant : public Robot.RobotUtility.FSPPredicate
          {               private Robot.GridCell origin;
               public SimGridAllyDeterminant(Robot.GridCell origin_) { origin = origin_; }

               public boolean validCell(Robot.GridCell potential_ally)
               {
                    if(potential_ally==origin)
                         return false;

                    switch(potential_ally.contents)
                    {
                    case GridObject.ALLY:
                         return true;
                    case GridObject.SELF:
                         if(!(potential_ally instanceof SimGridCell) || !(origin instanceof SimGridCell))
                              return false;
                         SimGridCell origin_downcast = (SimGridCell)(origin);
                         SimGridCell potential_ally_downcast = (SimGridCell)(potential_ally);
                         if(potential_ally.occupant_data!=null &&
                            potential_ally.occupant_data.player.equals(origin.occupant_data.player))
                              return true;
                    default:
                         return false;
                    }
               }
          }

     private static class RobotData
     {
          public SimGridCell assoc_cell;
          public Robot.Robot_Specs specs;
          public Robot.Robot_Status status;
          public Robot robot;
          public String player;

          //Build information
          public Robot.BuildStatus whatBuilding;
          public int investedPower;
          public SimGridCell invested_assoc_cell;

          //Buffered radio messages
          public List<byte[]> buffered_radio;
     }

     private static class SimGridCell extends Robot.GridCell
     {
          public RobotData occupant_data;
          public int wallforthealth;
     }

     //RoboSim environmental constants
     private final int WALL_HEALTH = 50;
     private final int WALL_DEFENSE = 10;

     //RoboSim execution data (world grid, turn order, GUI reference, etc.)
     private SimGridCell[][] worldGrid;
     private ArrayList<RobotData> turnOrder;
     private int turnOrder_pos;
     private SimulatorGUI gui;

     //Always good to have an RNG handy
     Random generator;

     /**Helper method to retrieve a subgrid of the world grid
      * @param x_left left x coordinate (inclusive)
      * @param y_up smaller y coordinate (inclusive)
      * @param x_right right x coordinate (inclusive)
      * @param y_down larger y coordinate (inclusive)
      * @return subgrid of the world grid (NOT copied or sanitized)
      */
     private SimGridCell[][] getSubGrid(int x_left, int y_up, int x_right, int y_down)
          {
               final int x_length = x_right - x_left + 1;
               final int y_height = y_down - y_up + 1;

               SimGridCell[][] to_return = new SimGridCell[x_length][y_height];
               for(int i=x_left; i<=x_right; i++)
                    for(int j=y_up; j<=y_down; j++)
                         to_return[i-x_left][j-y_up] = worldGrid[i][j];
          }

     /**Sanitizer to create a GridCell[][] 2D array to give to client.
      * @param simgrid SimGridCell 2D array with cells to sanitize
      * @param player String containing player's name.  Used to create
      *               sanitized grid as it would be seen from player's
      *               perspective.
      * @return sanitized grid or subgrid from player's perspective
      */
     private static GridCell[][] sanitizeGrid(SimGridCell[][] simgrid, String player)
          {
               GridCell[][] to_return = new GridCell[simgrid.length][simgrid[0].length];
               for(int i=0; i<simgrid.length; i++)
                    for(int j=0; j<simgrid.length; j++)
                    {
                         SimGridCell sanitized = simgrid[i][j].clone();
                         if(sanitized.contents==SimGridCell.GridObject.SELF)
                              if(sanitized.occupant_data.player.equals(player))
                                   sanitized.contents=SimGridCell.GridObject.ALLY;
                              else
                                   sanitized.contents=SimGridCell.GridObject.ENEMY;
                         sanitized.occupant_data=null;
                         sanitized.wallforthealth=0;
                         to_return[i][j] = sanitized;
                    }

               return to_return;
          }

     private static Robot_Specs checkSpecsValid(Robot_Specs proposed, String player, int skill_points) throws RoboSimExecutionException
          {
               if(proposed.attack + proposed.defense + proposed.power + proposed.charge != skill_points)
                    throw new RoboSimExecutionException("attempted to create invalid robot!",player);
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
      * @param obstacles number of obstacles on battlefield
      */
     public RoboSim(String[] combatants, int initial_robots_per_combatant, int skill_points, int length, int width, int obstacles) throws RoboSimExecutionException
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
               generator = new Random();

               //Initialize array to hold turn order
               turnOrder = new ArrayList<RobotData>(combatants.length*initial_robots_per_combatant);

               //Position in turnOrder
               turnOrder_pos = 0;

               //Add robots for each combatant
               for(String player : combatants)
               {
                    Constructor<Robot> gen_robot = Class.forName(player).getConstructor();
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
                         data.assoc_cell = worldGrid[x_pos][y_pos];
                         data.robot = gen_robot.newInstance();
                         data.player = player;
                         byte[] creation_message = new byte[64];
                         byte[1] = turnOrder_pos % 256;
                         byte[0] = turnOrder_pos / 256;
                         data.specs = checkSpecsValid(data.robot.createRobot(null, skill_points, creation_message), player, skill_points);
                         data.status = new Robot.Robot_Status();
                         data.status.charge = data.status.health = data.specs.power*10;
                         data.buffered_radio = new ArrayList<byte[]>();
                         turnOrder.set(turnOrder_pos++,data);
                    }
               }

               //Add obstacles to battlefield
               for(int i=0 i<obstacles; i++)
               {
                    do
                    {
                         x_pos = generator.nextInt(length);
                         y_pos = generator.nextInt(width);
                    } while(worldGrid[x_pos][y_pos].contents!=SimGridCell.GridObject.EMPTY);
                    worldGrid[x_pos][y_pos].contents = SimGridCell.GridObject.WALL;
                    worldGrid[x_pos][y_pos].wallforthealth = WALL_HEALTH;
               }
          }

     /**
      * The implementing class for the WorldAPI reference.
      * We can't just use ourselves for this because students
      * could downcast us to our real type and manipulate the
      * simulator in untoward ways (not that any of you would
      * do that, and I would catch it when I reviewed your
      * code anyway, but still).
      * 
      * NOTE: THIS IS A *NON-STATIC* INNER CLASS!
      */
     private class RoboAPIImplementor implements WorldAPI
     {
          private RobotData actingRobot;

          /**
           * Private constructor
           * @param actingRobot_ data for robot attempting to act in the simulator
           */
          private RoboAPIImplementor(RobotData actingRobot_) { actingRobot = actingRobot_; }

          /**
           * Utility method to check whether student's robot is adjacent to cell it is attacking.
           * Note: diagonal cells are not adjacent.
           * @param adjacent_cell cell to compare with student's robot's position
           */
          private boolean isAdjacent(Robot.GridCell adjacent_cell)
               {
                    return (Math.abs(actingRobot.assoc_cell.x_coord-adjacent_cell.x_coord)==1 &&
                              actingRobot.assoc_cell.y_coord == adjacent_cell.y_coord ||
                            Math.abs(actingRobot.assoc_cell.y_coord-adjacent_cell.y_coord)==1 &&
                              actingRobot.assoc_cell.x_coord == adjacent_cell.x_coord);
               }

          /**
           * Did the attacker hit the defender?
           * @param attack attack skill of attacker (including bonuses/penalties)
           * @param defense defense skill of defender (including bonuses)
           * @return whether the attacker hit
           */
          private boolean calculateHit(int attack, int defense)
               {
                    int luckOfAttacker = generator.nextInt(10);
                    return luckOfAttacker+attack-defense>=5;
               }

          /**
           * Process attack, assigning damage and deleting destroyed objects if necessary.
           * @param attack attack skill of attacker (including bonuses/penalties)
           * @param cell_to_attack cell attacker is attacking containing enemy or obstacle
           * @param damage damage if attack hits
           */
          private Robot.AttackResult processAttack(int attack, SimGridCell cell_to_attack, int power)
               {
                    //Holds result of attack
                    Robot.AttackResult to_return = Robot.AttackResult.MISSED;

                    //Calculate defense skill of opponent
                    int defense = 0;
                    switch(cell_to_attack.contents)
                    {
                    case SimGridCell.GridObject.SELF:
                         defense = cell_to_attack.occupant_data.specs.defense + cell_to_attack.occupant_data.status.defense_boost;
                         break;

                    case SimGridCell.GridObject.FORT:
                    case SimGridCell.GridObject.WALL:
                         defense = 10;
                         break;
                    }


                    //If we hit, damage the opponent
                    if(calculateHit(attack,defense))
                    {
                         //We hit
                         to_return = Robot.AttackResult.HIT;

                         if(cell_to_attack.occupant_data!=null)
                         {
                              //we're a robot
                              for(int i=0; true; i++)
                                        if(turnOrder.get(i)==cell_to_attack.occupant_data.robot)
                                             if((cell_to_attack.occupant_data.status-=power)<=0)
                                             {
                                                  //We destroyed the opponent!
                                                  to_return = Robot.AttackResult.DESTROYED_TARGET;

                                                  //Handle in-progress build, reusing setBuildTarget() to handle interruption of build due to death
                                                  (new RoboAPIImplementor(cell_to_attack.occupant_data)).setBuildTarget(null,null);

                                                  //Handle cell
                                                  cell_to_attack.occupant_data = null;
                                                  cell_to_attack.contents = cell_to_attack.wallforthealth>0 ? SimGridCell.GridObject.FORT : SimGridCell.GridObject.EMPTY;

                                                  //Handle turnOrder position
                                                  turnOrder.erase(i);
                                                  if(i<turnOrder_pos)
                                                       turnOrder_pos--;

                                                  break;
                                             }
                         }
                         else
                              if((cell_to_attack.wallforthealth-=power)<=0)
                              {
                                   //We destroyed the target!
                                   to_return = Robot.AttackResult.DESTROYED_TARGET;

                                   cell_to_attack.wallforthealth = 0;
                                   cell_to_attack.contents = SimGridCell.GridObject.EMPTY;
                              }
                    }

                    return to_return;
               }

          public Robot.AttackResult meleeAttack(int power, Robot.GridCell adjacent_cell) throws RoboSimExecutionException
               {
                    //Lots of error checking here (as everywhere...)
                    if(adjacent_cell==null)
                         throw new RoboSimExecutionException("passed null as argument to meleeAttack()",actingRobot.player);

                    //Check that we're using a valid amount of power
                    if(power > actingRobot.status.power || power > actingRobot.specs.attack || power < 1)
                         throw new RoboSimExecutionException("attempted melee attack with illegal power level",actingRobot.player,actingRobot.assoc_cell);

                    //Are cells adjacent?
                    if(!isAdjacent(adjacent_cell))
                         throw new RoboSimExecutionException("attempted to melee attack nonadjacent cell",actingRobot.player,actingRobot.assoc_cell);

                    //Does cell exist in grid?
                    //(could put this in isAdjacent() method but want to give students more useful error messages)
                    if(adjacent_cell.x_coord > worldGrid.length || adjacent_cell.y_coord > worldGrid[0].length ||
                       adjacent_cell.x_coord < 0 || adjacent_cell.y_coord < 0)
                         throw new RoboSimExecutionException("passed invalid cell coordinates to meleeAttack()",actingRobot.player,actingRobot.assoc_cell,adjacent_cell);

                    //Safe to use this now, checked for oob condition from student
                    SimGridCell cell_to_attack = worldGrid[adjacent_cell.x_coord][adjacent_cell.y_coord];

                    //Is there an enemy, fort, or wall at the cell's location?
                    switch(cell_to_attack.contents)
                    {
                    case SimGridCell.GridObject.EMPTY:
                         throw new RoboSimExecutionException("attempted to attack empty cell",actingRobot.player,actingRobot.assoc_cell,cell_to_attack);
                    case SimGridCell.BLOCKED:
                         throw new RoboSimExecutionException("attempted to attack blocked tile",actingRobot.player,actingRobot.assoc_cell,cell_to_attack);
                    case SimGridCell.GridObject.SELF:
                         if(cell_to_attack.occupant_data.player.equals(actingRobot.player))
                              throw new RoboSimExecutionException("attempted to attack ally",actingRobot.player,actingRobot.assoc_cell,cell_to_attack);
                    case SimGridCell.GridObject.CAPSULE:
                         throw new RoboSimExecutionException("attempted to attack energy capsule",actingRobot.player,actingRobot.assoc_cell,cell_to_attack);
                    case SimGridCell.GridObject.ALLY:
                         throw new RuntimeException("ERROR in RoboSim.RoboAPIImplementor.meleeAttack().  This is probably not the student's fault.  Contact Patrick Simmons about this message.  (Not the Doobie Brother...)");
                    }
                    
                    //Okay, if we haven't thrown an exception, the cell is valid to attack.  Perform the attack.
                    //Update this robot's charge status and power status.
                    actingRobot.status.charge-=power;
                    actingRobot.status.power-=power;

                    //Begin calculation of our attack power
                    int raw_attack = actingRobot.specs.attack;

                    //If we're outside a fort attacking someone in the fort, range penalty applies
                    if(cell_to_attack.wallforthealth > 0 && cell_to_attack.occupant_data!=null)
                         raw_attack/=2;

                    //Attack adds power of attack to raw skill
                    int attack = raw_attack + power;

                    //Process attack
                    return processAttack(attack,cell_to_attack,power);
               }

          public Robot.AttackResult rangedAttack(int power, Robot.GridCell nonadjacent_cell) throws RoboSimExecutionException
               {
                    //Lots of error checking here (as everywhere...)
                    if(adjacent_cell==null)
                         throw new RoboSimExecutionException("passed null as argument to rangedAttack()");


                    //Check that we're using a valid amount of power
                    if(power > actingRobot.status.power || power > actingRobot.specs.attack || power < 1)
                         throw new RoboSimExecutionException("attempted ranged attack with illegal power level",actingRobot.player,actingRobot.assoc_cell);

                    //Does cell exist in grid?
                    //(could put this in isAdjacent() method but want to give students more useful error messages)
                    if(nonadjacent_cell.x_coord > worldGrid.length || nonadjacent_cell.y_coord > worldGrid[0].length ||
                       nonadjacent_cell.x_coord < 0 || nonadjacent_cell.y_coord < 0)
                         throw new RoboSimExecutionException("passed invalid cell coordinates to rangedAttack()",actingRobot.player,actingRobot.assoc_cell,nonadjacent_cell);

                    //Are cells nonadjacent?
                    if(isAdjacent(nonadjacent_cell))
                         throw new RoboSimExecutionException("attempted to range attack adjacent cell",actingRobot.player,actingRobot.assoc_cell);

                    //Safe to use this now, checked for oob condition from student
                    SimGridCell cell_to_attack = worldGrid[nonadjacent_cell.x_coord][nonadjacent_cell.y_coord];

                    //Do we have a "clear shot"?
                    List<Robot.GridCell> shortest_path = Robot.RobotUtility.findShortestPath(actingRobot.assoc_cell,cell_to_attack,worldGrid);
                    if(shortest_path==null) //we don't have a clear shot
                         throw new RoboSimExecutionException("attempted to range attack cell with no clear path",actingRobot.player,actingRobot.assoc_cell,cell_to_attack);
                    else if(shortest_path.size()>actingRobot.specs.defense) //out of range
                         throw new RoboSimExecutionException("attempted to range attack cell more than (defense) tiles away",actingRobot.player,actingRobot.assoc_cell,cell_to_attack);

                    //Is there an enemy, fort, or wall at the cell's location?
                    switch(cell_to_attack.contents)
                    {
                    case SimGridCell.GridObject.EMPTY:
                         throw new RoboSimExecutionException("attempted to attack empty cell",actingRobot.player,actingRobot.assoc_cell,cell_to_attack);
                    case SimGridCell.BLOCKED:
                         throw new RoboSimExecutionException("attempted to attack blocked tile",actingRobot.player,actingRobot.assoc_cell,cell_to_attack);
                    case SimGridCell.GridObject.SELF:
                         if(cell_to_attack.occupant_data.player.equals(actingRobot.player))
                              throw new RoboSimExecutionException("attempted to attack ally",actingRobot.player,actingRobot.assoc_cell,cell_to_attack);
                    case SimGridCell.GridObject.CAPSULE:
                         throw new RoboSimExecutionException("attempted to attack energy capsule",actingRobot.player,actingRobot.assoc_cell,cell_to_attack);
                    case SimGridCell.GridObject.ALLY:
                         throw new RuntimeException("ERROR in RoboSim.RoboAPIImplementor.rangedAttack().  This is probably not the student's fault.  Contact Patrick Simmons about this message.  (Not the Doobie Brother...)");
                    }

                    //Okay, if we haven't thrown an exception, the cell is valid to attack.  Perform the attack.
                    //Update this robot's charge status.
                    actingRobot.status.charge-=power;
                    actingRobot.status.power-=power;

                    //Begin calculation of our attack power
                    int raw_attack = actingRobot.specs.attack/2;

                    //Attack adds power of attack to raw skill
                    int attack = raw_attack + power;

                    //Process attack
                    return processAttack(attack,cell_to_attack,power);
               }

          public Robot.AttackResult capsuleAttack(int power_of_capsule, Robot.GridCell cell)
               {
                    //Error checking, *sigh*...
                    if(cell==null)
                         throw new RoboSimExecutionException("passed null to capsuleAttack()",actingRobot.player,actingRobot.assoc_cell);

                    //Does cell exist in grid?
                    if(cell.x_coord > worldGrid.length || cell.y_coord > worldGrid[0].length || cell.x_coord < 0 || cell.y_coord < 0)
                         throw new RoboSimExecutionException("passed invalid cell coordinates to capsuleAttack()",actingRobot.player,actingRobot.assoc_cell,cell);

                    //Cell to attack
                    GridCell cell_to_attack = worldGrid[cell.x_coord][cell.y_coord];

                    //Do we have a capsule of this power rating?
                    int capsule_index = ArrayUtility.linearSearch(actingRobot.status.capsules,power_of_capsule);

                    if(capsule_index==-1)
                         throw new RoboSimExecutionException("passed invalid power to capsuleAttack(): doesn't have capsule of power "+power_of_capsule,actingRobot.player,actingRobot.assoc_cell);

                    //Can we use this capsule? (attack + defense >= power)
                    if(actingRobot.specs.attack + actingRobot.specs.defense < power_of_capsule)
                         throw new RoboSimExecutionException("attempted to use capsule of greater power than attack+defense",actingRobot.player,actingRobot.assoc_cell);

                    //Can we hit the target?  Range is power of capsule + defense.
                    if(Robot.RobotUtility.findShortestPath(actingRobot.assoc_cell,cell_to_attack,grid).length > power_of_capsule + actingRobot.specs.defense)
                         throw new RoboSimExecutionException("target not in range",actingRobot.player,actingRobot.assoc_cell,cell_to_attack);

                    //Is there an enemy, fort, or wall at the cell's location?
                    switch(cell_to_attack.contents)
                    {
                    case SimGridCell.GridObject.EMPTY:
                         throw new RoboSimExecutionException("attempted to attack empty cell",actingRobot.player,actingRobot.assoc_cell,cell_to_attack);
                    case SimGridCell.BLOCKED:
                         throw new RoboSimExecutionException("attempted to attack blocked tile",actingRobot.player,actingRobot.assoc_cell,cell_to_attack);
                    case SimGridCell.GridObject.SELF:
                         if(cell_to_attack.occupant_data.player.equals(actingRobot.player))
                              throw new RoboSimExecutionException("attempted to attack ally",actingRobot.player,actingRobot.assoc_cell,cell_to_attack);
                    case SimGridCell.GridObject.CAPSULE:
                         throw new RoboSimExecutionException("attempted to attack energy capsule",actingRobot.player,actingRobot.assoc_cell,cell_to_attack);
                    case SimGridCell.GridObject.ALLY:
                         throw new RuntimeException("ERROR in RoboSim.RoboAPIImplementor.capsuleAttack().  This is probably not the student's fault.  Contact Patrick Simmons about this message.  (Not the Doobie Brother...)");
                    }

                    /*Okay, if we're still here, we can use the capsule.
                      Need to delete capsule from robot status structure.
                      (We're using arrays here b/c of 1301).
                    */
                    actingRobot.status.capsules = ArrayUtility.deleteElement(actingRobot.status.capsules,capsule_index);

                    int[] newCapsules = new int[actingRobot.status.capsules.length-1];
                    boolean deleted_capsule = false;
                    for(int i=0,j=0; i<actingRobot.status.capsules.length; i++,j++)
                    {
                         if(!deleted_capsule && actingRobot.status.capsules[i]==power_of_capsule)
                         {
                              deleted_capsule=true;
                              j--;
                              continue;
                         }
                         newCapsules[j]=actingRobot.status.capsules[i];
                    }

                    //Process attack
                    return processAttack(attack + power_of_capsule,cell_to_attack,(int)(Math.ceil(0.1 * power_of_capsule * actingRobot.specs.attack)));
               }

          public void defend(int power)
               {
                    //Error checking
                    if(power < 0 || power > actingRobot.specs.defense || power > actingRobot.specs.power || power > actingRobot.status.charge)
                         throw new RoboSimExecutionException("attemped to defend with negative power",actingRobot.player, actingRobot.assoc_cell);

                    //This one's easy
                    actingRobot.status.charge-=power;
                    actingRobot.status.defense_boost+=power;
               }

          public void move(int steps, Robot.Direction way)
               {
                    int x_coord = actingRobot.assoc_cell.x_coord;
                    final int actor_x = x_coord;
                    int y_coord = actingRobot.assoc_cell.y_coord;
                    final int actor_y = y_coord;
                    switch(way)
                    {
                    case Robot.Direction.UP:
                         y_coord+=steps;
                         break;
                    case Robot.Direction.DOWN:
                         y_coord-=steps;
                         break;
                    case Robot.Direction.LEFT:
                         x_coord-=steps;
                         break;
                    case Robot.Direction.RIGHT:
                         x_coord+=steps;
                         break;
                    }

                    //Is our destination in the map?
                    if(x_coord < 0 || x_coord > worldGrid.length || y_coord < 0 || y_coord > worldGrid[0].length)
                         throw new RoboSimExecutionException("attempted to move out of bounds",actingRobot.player,actingRobot.assoc_cell);

                    //Okay, now we have to make sure each step is empty
                    final boolean x_left = x_coord<actor_x;
                    final boolean y_left = y_coord<actor_y;
                    if(x_coord!=actor_x)
                         for(int i=(x_left ? actor_x-1 : actor_x+1); i!=x_coord; x_left ? x-- : x++)
                              if(worldGrid[x_coord][y_coord].contents!=SimGridCell.GridObject.EMPTY)
                                   throw new RoboSimExecutionException("attempted to cross illegal cell",actingRobot.player,actingRobot.assoc_cell,worldGrid[x_coord][y_coord]);

                    //Okay, now: do we have enough power/charge?
                    if(steps > actingRobot.status.power)
                         throw new RoboSimExecutionException("attempted to move too far (not enough power)",actingRobot.player,actingRobot.assoc_cell,worldGrid[x_coord][y_coord]);

                    //Account for power cost
                    actingRobot.status.power-=steps;
                    actingRobot.status.charge-=steps;

                    //Change position of robot.
                    actingRobot.assoc_cell.contents = SimGridCell.GridObject.EMPTY;
                    actingRobot.assoc_cell.occupant_data = NULL;
                    actingRobot.assoc_cell = worldGrid[x_coord][y_coord];
                    actingRobot.assoc_cell.contents = SimGridCell.GridObject.SELF;
                    actingRobot.assoc_cell.occupant_data = actingRobot;
               }

          public void pick_up_capsule(Robot.GridCell adjacent_cell)
               {
                    //Error checking, *sigh*...
                    //Can't pass us null
                    if(adjacent_cell==null)
                         throw new RoboSimExecutionException("passed null to pick_up_capsule()",actingRobot.player,actingRobot.assoc_cell);

                    //Does cell exist in grid?
                    if(adjacent_cell.x_coord > worldGrid.length || adjacent_cell.y_coord > worldGrid[0].length || adjacent_cell.x_coord < 0 || adjacent_cell.y_coord < 0)
                         throw new RoboSimExecutionException("passed invalid cell coordinates to pick_up_capsule()",actingRobot.player,actingRobot.assoc_cell,adjacent_cell);

                    //Cell in question
                    SimGridCell gridCell = worldGrid[cell.x_coord][cell.y_coord];

                    //Cell must be adjacent
                    if(!isAdjacent(adjacent_cell))
                         throw new RoboSimExecutionException("attempted to pick up capsule in nonadjacent cell",actingRobot.player,actingRobot.assoc_cell,gridCell);

                    //We need at least one power.
                    if(actingRobot.status.power==0)
                         throw new RoboSimExecutionException("attempted to pick up capsule with no power",actingRobot.player,actingRobot.assoc_cell,gridCell);

                    //Is there actually a capsule there?
                    if(gridCell.contents!=GridCell.contents.CAPSULE)
                         throw new RoboSimExecutionException("attempted to pick up capsule from cell with no capsule",actingRobot.player,actingRobot.assoc_cell,gridCell);

                    //Do we have "room" for this capsule?
                    if(actingRobot.status.capsules.length+1>actingRobot.specs.attack+actingRobot.specs.defense)
                         throw new RoboSimExcecutionException("attempted to pick up too many capsules",actingRobot.player,actingRobot.assoc_cell,gridCell);

                    //If still here, yes.

                    //Decrement our power
                    actingRobot.status.power--;

                    //Put capsule in our inventory, delete it from world
                    actingRobot.status.capsules = ArrayUtility.addElement(actingRobot.status.capsules,gridCell.capsule_power);
                    gridCell.contents = SimGridCell.GridObject.EMPTY;
                    gridCell.capsule_power = 0;
               }

     public void drop_capsule(Robot.GridCell adjacent_cell, int power_of_capsule)
               {
                    //Error checking, *sigh*...
                    //Can't pass us null
                    if(adjacent_cell==null)
                         throw new RoboSimExecutionException("passed null to pick_up_capsule()",actingRobot.player,actingRobot.assoc_cell);

                    //Does cell exist in grid?
                    if(adjacent_cell.x_coord > worldGrid.length || adjacent_cell.y_coord > worldGrid[0].length || adjacent_cell.x_coord < 0 || adjacent_cell.y_coord < 0)
                         throw new RoboSimExecutionException("passed invalid cell coordinates to pick_up_capsule()",actingRobot.player,actingRobot.assoc_cell,adjacent_cell);

                    //Cell in question
                    SimGridCell gridCell = worldGrid[adjacent_cell.x_coord][adjacent_cell.y_coord];

                    //Cell must be adjacent
                    if(!isAdjacent(adjacent_cell))
                         throw new RoboSimExecutionException("attempted to pick up capsule in nonadjacent cell",actingRobot.player,actingRobot.assoc_cell,gridCell);

                    //Is the cell empty?
                    if(gridCell.contents!=SimGridCell.GridObject.EMPTY)
                         throw new RoboSimExcecutionException("attempted to place capsule in nonempty cell",actingRobot.player,actingRobot.assoc_cell,gridCell);

                    //Do we have such a capsule?
                    int index = ArrayUtility.linearSearch(actingRobot.status.capsules,power_of_capsule);
                    if(index==-1)
                         throw new RoboSimExecutionException("attempted to drop capsule with power "+power_of_capsule", having no such capsule",actingRobot.player,actingRobot.assoc_cell,gridCell);

                    //Okay.  We're good.  Drop the capsule
                    gridCell.contents = SimGridCell.GridObject.CAPSULE;
                    gridCell.capsule_power = power_of_capsule;

                    //Delete it from our inventory
                    ArrayUtility.deleteElement(actingRobot.status.capsules,index);
               }

          public Robot.BuildStatus getBuildStatus()
               {
                    return actingRobot.whatBuilding;
               }

          public cRobot.GridCell getBuildTarget()
               {
                    return actingRobot.invested_assoc_cell;
               }

          public int getInvestedBuildPower()
               {
                    return actingRobot.investedPower;
               }

          private void finalizeBuilding(byte[] creation_message)
               {
                    //Nothing to finalize if not building anything
                    if(actingRobot.whatBuilding==null)
                         return;

                    //What do we have to finalize?
                    switch(actingRobot.whatBuilding)
                    {
                    case Robot.BuildStatus.WALL:
                         if(actingRobot.investedPower >= WALL_HEALTH)
                              actingRobot.invested_assoc_cell.contents = SimGridCell.GridObject.WALL;
                         else
                              actingRobot.invested_assoc_cell.contents = SimGridCell.gridObject.EMPTY;
                         break;

                    case Robot.BuildStatus.FORT:
                         if(actingRobot.investedPower >= 75)
                              actingRobot.invested_assoc_cell.contents = SimGridCell.GridObject.FORT;
                         else
                              actingRobot.invested_assoc_cell.contents = SimGridCell.gridObject.EMPTY;
                         break;

                    case Robot.BuildStatus.CAPSULE:
                         int capsule_power = actingRobot.investedPower/10;
                         if(capsule_power!=0)
                         {
                              if(actingRobot.status.capsules.length+1>actingRobot.specs.attack+actingRobot.specs.defense)
                                   throw new RoboSimExcecutionException("attempted to finish building capsule when already at max capsule capacity",actingRobot.player,actingRobot.assoc_cell);
                              actingRobot.status.capsules = ArrayUtility.addElement(actingRobot.status.capsules,capsule_power);
                         }
                         break;

                    case Robot.BuildStatus.ROBOT:
                         int skill_points = actingRobot.investedPower/20;
                         if(skill_points!=0)
                         {
                              //Check creation message correct size
                              if(creation_message!=null && creation_message.size()!=64)
                                   throw new RoboSimExecutionException("passed incorrect sized creation message to setBuildTarget()",actingRobot.player,actingRobot.assoc_cell,actingRobot.invested_assoc_cell);

                              //Set default creation message if we don't have one
                              if(creation_message==null)
                              {
                                   creation_message = new byte[64];
                                   creation_message[1] = turnOrder.size() % 256;
                                   creation_message[0] = turnOrder.size() / 256;
                              }

                              //Create the robot
                              actingRobot.invested_assoc_cell.contents = SimGridCell.GridObject.SELF;
                              RobotData data = actingRobot.invested_assoc_cell.occupant_data = new RobotData();
                              data.assoc_cell = actingRobot.invested_assoc_cell;
                              data.robot = Class.forName(actingRobot.player).getConstructor().newInstance();
                              data.player = actingRobot.player;
                              data.specs = checkSpecsValid(data.robot.createRobot(null, skill_points, creation_message), actingRobot.player, skill_points);
                              data.status = new Robot.Robot_Status();
                              data.status.charge = data.status.health = data.specs.power*10;
                              data.bufferedRadio = new ArrayList<byte[]>();
                              turnOrder.add(data);
                         }
                         else
                              actingRobot.invested_assoc_cell.contents = SimGridCell.gridObject.EMPTY;
                         break;                              
                    }
               }

          public void setBuildTarget(Robot.BuildStatus status, Robot.GridCell location)
               {
                    setBuildTarget(status,location,null);
               }

          public void setBuildTarget(Robot.BuildStatus status, Robot.GridCell location, byte[] message)
               {
                    //If we're in the middle of building something, finalize it.
                    if(actingRobot.whatBuilding!=null)
                         finalizeBuilding(message);

                    //Update status
                    actingRobot.whatBuilding = status;
                    actingRobot.investedPower = 0;                    
                    actingRobot.invested_assoc_cell = location;

                    //Error checking, *sigh*...
                    //CAN pass us null, so special-case it
                    if(location==null)
                    {
                         //We must be building capsule, then.
                         if(actingRobot.whatBuilding!=null && actingRobot.whatBuilding!=Robot.BuildStatus.CAPSULE)
                              throw new RoboSimExecutionException("passed null to setBuildTarget() location with non-null and non-capsule build target",actingRobot.player,actingRobot.assoc_cell);
                         actingRobot.investedPower = 0;
                         return;
                    }

                    //If location NOT null, must not be building capsule
                    if(location!=null && (status == null || status == Robot.BuildStatus.CAPSULE))
                         throw new RoboSimExecutionException("attempted to target capsule or null building on non-null adjacent cell",actingRobot.player,actingRobot.assoc_cell,location);

                    //Does cell exist in grid?
                    if(location.x_coord > worldGrid.length || location.y_coord > worldGrid[0].length || location.x_coord < 0 || location.y_coord < 0)
                         throw new RoboSimExecutionException("passed invalid cell coordinates to setBuildTarget()",actingRobot.player,actingRobot.assoc_cell,location);

                    //Cell in question
                    SimGridCell gridCell = worldGrid[location.x_coord][location.y_coord];

                    //Cell must be adjacent
                    if(!isAdjacent(location))
                         throw new RoboSimExecutionException("attempted to set build target to nonadjacent cell",actingRobot.player,actingRobot.assoc_cell,gridCell);

                    //Is the cell empty?
                    if(gridCell.contents!=SimGridCell.GridObject.EMPTY)
                         throw new RoboSimExcecutionException("attempted to set build target to nonempty cell",actingRobot.player,actingRobot.assoc_cell,gridCell);

                    //Okay, block off cell since we're building there now.
                    gridCell.contents = SimGridCell.GridObject.BLOCKED;
               }

          public void build(int power)
               {
                    if(power > actingRobot.status.power || power < 0)
                         throw new RoboSimExecutionException("attempted to apply invalid power to build task",actingRobot.player,actingRobot.assoc_cell);
                    actingRobot.status.charge-=power;
                    actingRobot.status.power-=power;
                    actingRobot.investedPower+=power;
               }

          public void repair(int power)
               {
                    if(power > actingRobot.status.power || power < 0)
                         throw new RoboSimExecutionException("attempted to apply invalid power to repair task",actingRobot.player,actingRobot.assoc_cell);
                    actingRobot.status.charge-=power;
                    actingRobot.status.power-=power;
                    actingRobot.status.health+=power/2;
               }

          public void charge(int power, Robot.GridCell ally)
               {
                    //Lots of error checking here (as everywhere...)
                    if(ally==null)
                         throw new RoboSimExecutionException("passed null as argument to charge()",actingRobot.player);

                    //Check that we're using a valid amount of power
                    if(power > actingRobot.status.power || power < 1)
                         throw new RoboSimExecutionException("attempted charge with illegal power level",actingRobot.player,actingRobot.assoc_cell);

                    //Are cells adjacent?
                    if(!isAdjacent(ally))
                         throw new RoboSimExecutionException("attempted to charge nonadjacent cell",actingRobot.player,actingRobot.assoc_cell);

                    //Does cell exist in grid?
                    //(could put this in isAdjacent() method but want to give students more useful error messages)
                    if(ally.x_coord > worldGrid.length || ally.y_coord > worldGrid[0].length || ally.x_coord < 0 || ally.y_coord < 0)
                         throw new RoboSimExecutionException("passed invalid cell coordinates to charge()",actingRobot.player,actingRobot.assoc_cell,ally);

                    //Safe to use this now, checked for oob condition from student
                    SimGridCell allied_cell = worldGrid[ally.x_coord][ally.y_coord];

                    //Is there an ally in that cell?
                    if(allied_cell.contents!=SimGridCell.GridObject.SELF || !allied_cell.occupant_data.player.equals(actingRobot.player))
                         throw new RoboSimExecutionException("attempted to charge non-ally, or cell with no robot in it",actingRobot.player,actingRobot.assoc_cell,allied_cell);

                    //Perform the charge
                    actingRobot.status.power-=power;
                    actingRobot.status.charge-=power;
                    allied_cell.occupant_data.status.charge+=power;
               }

          public void sendMessage(byte[] message, int power)
               {
                    if(power < 1 || power > 2)
                         throw new RoboSimExecutionException("attempted to send message with invalid power", actingRobot.player,actingRobot.assoc_cell);

                    if(message.length!=64)
                         throw new RoboSimExecutionException("attempted to send message byte array of incorrect length", actingRobot.player,actingRobot.assoc_cell);

                    GridCell target = null;
                    if(power==1)
                    {
                         target = Robot.findNearestAlly(origin,grid);
                         if(target!=null)
                         {
                              /*There's a way to "cheat" here and set up a power-free comm channel
                               *between two allied robots.  If you can find it ... let me know, and
                               *you'll get extra credit :).  Additional credit for a bugfix.*/
                              ((SimGridCell)(target)).occupant_data.buffered_radio.add(message);
                         }
                         return;
                    }
                    else //power==2
                         for(RobotData x : turnOrder)
                              if(x!=actingRobot && x.player.equals(actingRobot.player))
                                   x.buffered_radio.add(message);
               }

          //It's a wonderful day in the neighborhood...
          public Robot.GridCell[][] getVisibleNeighborhood()
               {
                    //YAY!  No parameters means NO ERROR CHECKING!  YAY!
                    final int range = actingRobot.specs.defense;
                    final int xloc = actingRobot.assoc_cell.x_coord;
                    final int yloc = actingRobot.assoc_cell.y_coord;
                    final int x_left = (xloc - range < 0) ? 0 : (xloc - range);
                    final int x_right = (xloc + range > worldGrid.length-1) ? (worldGrid.length-1) : (xloc + range);
                    final int y_up = (yloc - range < 0) ? 0 : (yloc - range);
                    final int y_down = (yloc + range > worldGrid[0].length - 1) ? (worldGrid[0].length-1) : (yloc + range);
                    Robot.GridCell[][] to_return = sanitizeGrid(getSubGrid(x_left,y_up,x_right,y_down));

                    //Set associated cell to SELF instead of ALLY
                    to_return[xloc - x_left][yloc - y_up].contents=SimGridCell.GridObject.SELF;
                    return to_return;
               }

          public Robot.GridCell[][] getWorld(int power)
               {
                    if(power!=3)
                         throw new RoboSimExecutionException("tried to get world with invalid power (not equal to 3)",actingRobot.player,actingRobot.assoc_cell);

                    Robot.GridCell[][] to_return = sanitizeGrid(getSubGrid(0,0,worldGrid.length,worldGrid[0].length));

                    //Set self to self instead of ally
                    to_return[xloc - x_left][yloc - y_up].contents=SimGridCell.GridObject.SELF;
                    return to_return;
               }

          public void scanEnemy(Robot.Robot_Specs enemySpecs, Robot.Robot_Status enemyStatus, Robot.GridCell toScan)
               {
               }
     }

     /**
      * Executes one timestep of the simulation.
      * @return the winner, if any, or null
      */
     public String executeSingleTimeStep() throws RoboSimExecutionException
          {
               
          }
}
