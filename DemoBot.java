/**DemoBot:
 * Demo Robot implementation for OffenseBot
 */

public class DemoBot implements Robot
{
     private Robot_Specs my_specs;

     public Robot_Specs createRobot(WorldAPI api, int skill_points, byte[] message)
          {
               Robot_Specs to_return = new Robot_Specs();
               to_return.attack = to_return.defense = to_return.power = to_return.charge = skill_points/4;
               //This handles the pathological case where skill_points<4
               if(skill_points<4)
               {
                    to_return.charge = 1;
                    skill_points--;
                    if(skill_points > 0)
                    {
                         to_return.power = 1;
                         skill_points--;
                    }
                    if(skill_points > 0)
                    {
                         to_return.defense = 1;
                         skill_points--;
                    }
               }
               else
               {
                    skill_points-=(skill_points/4)*4;
                    to_return.attack += skill_points;
               }

               //Keep track of our specs; simulator won't do it for us!
               my_specs = to_return;
               return to_return;
          }

     private static boolean isAdjacent(GridCell c1, GridCell c2)
          {
               return (Math.abs(c1.x_coord-c2.x_coord)==1 &&
                       c1.y_coord == c2.y_coord ||
                       Math.abs(c1.y_coord-c2.y_coord)==1 &&
                       c1.x_coord == c2.x_coord);
          }
     
     private static int searchAndDestroy(GridCell self, GridCell[][] neighbors,WorldAPI api,int remaining_power) throws RoboSim.RoboSimExecutionException
          {
               System.out.println("Self: ("+self.x_coord+","+self.y_coord+")");
               for(int i=0; i<neighbors.length; i++)
                    for(int j=0; j<neighbors[0].length; j++)
                         if(neighbors[i][j].contents==GridObject.ENEMY)
                         {
                              GridCell[] path = RobotUtility.findShortestPath(self,neighbors[i][j],neighbors);
                              if(path!=null)
                                   for(int k=0; k<path.length-1 && remaining_power > 0; k++)
                                   {
                                        System.out.println("Move: ("+path[k].x_coord+","+path[k].y_coord+")");
                                        Direction way = null;
                                        if(path[k].x_coord < self.x_coord)
                                             way = Direction.LEFT;
                                        else if(path[k].x_coord > self.x_coord)
                                             way = Direction.RIGHT;
                                        else if(path[k].y_coord < self.y_coord)
                                             way = Direction.UP;
                                        else
                                             way = Direction.DOWN;
                                        api.move(1,way);
                                        remaining_power--;
                                        self = path[k];
                                   }

                              if(remaining_power > 0 && isAdjacent(self,neighbors[i][j]))
                              {
                                   api.meleeAttack(remaining_power,neighbors[i][j]);
                                   remaining_power = 0;
                              }
                         }

               return remaining_power;
          }

     public void act(WorldAPI api, Robot_Status status, byte[][] received_radio)
          {
               int remaining_power = status.power;
               int remaining_charge = status.charge;

               try
               {
                    GridCell[][] neighbors = api.getVisibleNeighborhood();

                    //What's our position?
                    GridCell self=null;
                    for(int i=0; i<neighbors.length; i++)
                         for(int j=0; j<neighbors[0].length; j++)
                              if(neighbors[i][j].contents==GridObject.SELF)
                              {
                                   self=neighbors[i][j];
                                   break;
                              }

                    //Visible and reachable enemy?  Attack!
                    remaining_power = searchAndDestroy(self,neighbors,api,remaining_power);
                    
                    //Do we still have power?  Then we haven't attacked anything.
                    //Perhaps we couldn't find an enemy...
                    if(remaining_power > 3)
                    {
                         GridCell[][] world = api.getWorld(3);
                         remaining_power-=3;
                         searchAndDestroy(self,world,api,remaining_power);
                    }
               }
               catch(RoboSim.RoboSimExecutionException e)
               {
                    System.err.println(e.getMessage());
               }
          }
}
 
