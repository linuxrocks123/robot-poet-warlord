/**
 * Robot Interface:
 * Your code must implement this simple interface in order to be
 * useable by the simulator.
 */
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
public interface Robot
{
     /** Represents skill point allocation of Robot.*/
     public class Robot_Specs implements Cloneable
     {
          /**Attack skill.*/
          public int attack;

          /**Defense skill.*/
          public int defense;

          /**Power skill.*/
          public int power;

          /**Charge skill.*/
          public int charge;

          /**override clone and make it public (to implement Cloneable)*/
          public Object clone() throws CloneNotSupportedException
               { return super.clone(); }
     }

     /**
      * Represents current status of Robot, passed to Robot.act() method
      * to let robot know its current health, charge, and capsules.
      */
     public class Robot_Status implements Cloneable
     {
          /**remaining power for turn*/
          public int power;

          /**current charge.*/
          public int charge;

          /**current health.*/
          public int health;

          /**current defense boost*/
          public int defense_boost;

          /**
           * Current number and power of capsules.<br>
           * Each capsule is represented by an element of the array.<br>
           * The value of the element of the array is equal to the power
           * of the capsule.<br>
           * In-progress capsules are not represented;
           * use getInvestedBuildPower() for that.
           */
          public int[] capsules;

          /**Constructor because I'd rather not capsules ever be null*/
          public Robot_Status() { capsules = new int[0]; }

          /**Overrides clone: see Java language documentation for explanation*/
          public Object clone() throws CloneNotSupportedException
               {
                    Robot_Status to_return = (Robot_Status)(super.clone());
                    to_return.capsules = capsules.clone();
                    return to_return;
               }
     };

     /**Represents object located in particular GridCell.*/
     public enum GridObject { EMPTY, BLOCKED, SELF, ALLY, ENEMY, WALL, FORT, CAPSULE };

     /**Represents orientation in simulator's world.*/
     public enum Direction { UP, DOWN, LEFT, RIGHT };

     /**Represents cell in grid of simulator's world.*/
     public class GridCell implements Cloneable
     {
          /**X-coordinate of cell.*/
          public int x_coord;

          /**Y-coordinate of cell.*/
          public int y_coord;

          /**contents of cell.*/
          public GridObject contents;

          /**orientation of fort, if there is a fort in the cell.*/
          public Direction fort_orientation;

          /**power of capsule, if there is a capsule in the cell.*/
          public int capsule_power;

          /**override clone and make it public (to implement Cloneable)*/
          public Object clone() throws CloneNotSupportedException
               { return super.clone(); }
     }

     /**Attack type: melee, ranged, or capsule*/
     public enum AttackType { MELEE, RANGED, CAPSULE };

     /**Class representing information about a particular attack or
      * attempted attack you suffered the previous round.*/
     public class AttackNotice
     {
          /**Cell from which the attack originated.*/
          GridCell origin;

          /**Form of the attack.*/
          AttackType form;
          
          /**Amount of damage suffered from attack (0 means attack failed)*/
          int damage;
     }

     /**Represents result of attack: missed, hit, destroyed target*/
     public enum AttackResult { MISSED, HIT, DESTROYED_TARGET };

     /**RobotUtility class<br>
      * Provides utilities for students to use in Robot implementations.<br>
      */
     public class RobotUtility
     {
          /**FSPPredicate class: used internally in nearest path methods*/
          public abstract static class FSPPredicate
          {
               public abstract boolean validCell(GridCell cell);
          }

          /**Find nearest neighbor:<br>
           * Finds the nearest ally to cell, approximately as the crow flies.
           * @param origin cell to find
           * @param grid grid to analyze
           * @return nearest ally, or null if we have no allies in grid
           */
          public static GridCell findNearestAlly(GridCell origin, GridCell[][] grid)
               {
                    GridCell[] path = findShortestPathInternal(origin, new RoboSim.SimGridAllyDeterminant(origin), new FSPPredicate() {
                              public boolean validCell(GridCell cell) { return true; }}, grid);

                    if(path==null)
                         return null;
                    else
                         return path[path.length-1];
               }

          /**Shortest path calculator:<br>
           * Finds the shortest path from one grid cell to another.<br><br>
           * This uses Dijkstra's algorithm to attempt to find the shortest
           * path from one grid cell to another.  Cells are adjacent if they
           * are up, down, left, or right of each other.  Cells are
           * <i>not</i> adjacent if they are diagonal to one another.<br>
           * @param origin starting grid cell
           * @param target ending grid cell
           * @param grid grid to analyze
           * @return a path guaranteed to be the shortest path from the
           *         origin to the target.  Will return null if no path
           *         could be found in the given grid.
           */
          public static GridCell[] findShortestPath(GridCell origin, final GridCell target, GridCell[][] grid)
               {
                    return findShortestPathInternal(origin,new FSPPredicate () {
                              public boolean validCell(GridCell maybeTarget)
                                   {
                                        return maybeTarget==target;
                                   }
                         },
                         new FSPPredicate() {
                              public boolean validCell(GridCell maybePassable)
                                   {
                                        return maybePassable.contents==GridObject.EMPTY;
                                   }
                         },
                         grid);
               }

          private static GridCell[] findShortestPathInternal(GridCell origin, FSPPredicate isTarget, FSPPredicate isPassable, GridCell[][] grid)
               {
                    //Offsets to handle incomplete world map
                    int x_offset = grid[0][0].x_coord;
                    int y_offset = grid[0][0].y_coord;
                    
                    //Structures to efficiently handle accounting
                    TreeMap<Integer,LinkedList<GridCell>> unvisited_nodes = new TreeMap<Integer,LinkedList<GridCell>>(); //this is really a multimap
                    Map<GridCell,LinkedList<GridCell>> current_costs = new HashMap<GridCell,LinkedList<GridCell>>();

                    //Origin's shortest path is simply itself
                    LinkedList<GridCell> origin_path = new LinkedList<GridCell>();
                    origin_path.add(origin);

                    //Initialize graph search with origin
                    current_costs.put(origin,origin_path);
                    unvisited_nodes.put(0,origin_path);

                    while(unvisited_nodes.size()!=0)
                    {
                         Map.Entry<Integer,LinkedList<GridCell>> current_entry = unvisited_nodes.firstEntry();
                         int our_cost = current_entry.getKey();
                         GridCell our_cell = current_entry.getValue().pop();
                         LinkedList<GridCell> current_path = current_costs.get(our_cell);

                         //If we are a destination, algorithm has finished: return our current path
                         if(isTarget.validCell(our_cell))
                              return current_path.toArray(new GridCell[0]);

                         //Erase our cost mapping from unvisited_nodes if necessary
                         if(current_entry.getValue().size()==0)
                              unvisited_nodes.remove(our_cost);

                         /*We are adjacent to up to 4 nodes, with
                           coordinates relative to ours as follows:
                           (-1,0),(1,0),(0,-1),(0,1)*/
                         LinkedList<GridCell> adjacent_nodes = new LinkedList<GridCell>();
                         int gridX_value = our_cell.x_coord - x_offset;
                         int gridY_value = our_cell.y_coord - y_offset;
                         if(gridX_value!=0)
                              adjacent_nodes.add(grid[gridX_value-1][gridY_value]);
                         if(gridX_value!=grid.length-1)
                              adjacent_nodes.add(grid[gridX_value+1][gridY_value]);
                         if(gridY_value!=0)
                              adjacent_nodes.add(grid[gridX_value][gridY_value-1]);
                         if(gridY_value!=grid[0].length-1)
                              adjacent_nodes.add(grid[gridX_value][gridY_value+1]);

                         /*Iterate over adjacent nodes, add to or update
                           entry in unvisited_nodes and current_costs if
                           necessary*/
                         for(GridCell x : adjacent_nodes)
                         {
                              //If we're not passable, we can't be used as an adjacent cell, unless we're a target
                              if(!isPassable.validCell(x) && !isTarget.validCell(x))
                                   continue;

                              //Generate proposed path
                              LinkedList<GridCell> x_proposed_path = (LinkedList<GridCell>)(current_path.clone());
                              x_proposed_path.add(x);

                              //current least cost path
                              List<GridCell> clcp = current_costs.get(x);

                              if(clcp!=null && clcp.size()-1>our_cost+1)
                              {
                                   LinkedList<GridCell> old_unvisited = unvisited_nodes.get(clcp.size()-1);
                                   old_unvisited.removeFirstOccurrence(x);
                                   if(old_unvisited.size()==0)
                                        unvisited_nodes.remove(clcp.size()-1);
                              }
                              else if(clcp!=null && clcp.size()-1<=our_cost+1)
                                   continue;

                              LinkedList<GridCell> new_unvisited = unvisited_nodes.get(our_cost+1);
                              if(new_unvisited==null)
                              {
                                   new_unvisited = new LinkedList<GridCell>();
                                   unvisited_nodes.put(our_cost+1,new_unvisited);
                              }
                              new_unvisited.add(x);
                              current_costs.put(x,x_proposed_path);
                         }
                    }

                    /*Loop is over, and we didn't find a path to the target :(
                      Return null.*/
                    return null;
               }
     }

     /**
      * Entry point for your robot on its creation
      * @param api a reference to a WorldAPI object you can use to
      *            interact with the simulator (currently unused)
      * @param skill_points the number of skill points your robot is
      *                     allowed to have.
      * @param message a 64-byte message from the robot who created you.
      *                If you were created by the simulator, the first two
      *                bytes of the message will contain your ID, which is
      *                unique among the IDs of all your team's robots
      *                created by the world.  Otherwise, the format of the
      *                message is unspecified: it's up to you to define it.
      * @return You are to return a Robot_Specs object containing the
      *         allocation of skill points you have chosen for yourself.
      */
     Robot_Specs createRobot(WorldAPI api, int skill_points, byte[] message);

     /**Represents what a robot is currently building*/
     enum BuildStatus { WALL, FORT, CAPSULE, ROBOT };

     /**
      * Each turn, this method is called to allow your robot to act.
      * @param api a reference to a WorldAPI object you can use to
      *            interact with the simulator.
      * @param status a reference to a Robot_Status object containing
      *               information about your current health and energy level
      * @param received_radio the radio signals you have received this
      *                       round.  Each message is exactly 64 bytes long.
      *                       You may be able to receive additional radio
      *                       signals by calling getMessages() with a
      *                       nonzero power if you are being jammed.
      */
     void act(WorldAPI api, Robot_Status status, byte[][] received_radio);
}
