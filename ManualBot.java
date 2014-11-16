/**ManualBot:
 * Robot implementation which asks the user for everything.<br>
 * This is VERY rough at the moment.
 */
import java.util.Scanner;
public class ManualBot implements Robot
{
     public static Scanner scanner;

     public ManualBot()
          {
               if(scanner==null)
                    scanner = new Scanner(System.in);
          }

     public Robot_Specs createRobot(WorldAPI api, int skill_points, byte[] message)
          {
               Robot_Specs to_return = new Robot_Specs();
               System.out.println("***Creation***\nSkill points: "+skill_points);
               try
               {
                    System.out.print("Attack? ");
                    to_return.attack = Integer.parseInt(scanner.nextLine());
                    System.out.print("Defense? ");
                    to_return.defense = Integer.parseInt(scanner.nextLine());
                    System.out.print("Power? ");
                    to_return.power = Integer.parseInt(scanner.nextLine());
                    System.out.print("Charge? ");
                    to_return.charge = Integer.parseInt(scanner.nextLine());
               }
               catch(NumberFormatException e)
               {
                    System.out.println(e.getMessage());
               }

               //TODO: handle message

               return to_return;
          }

     private GridCell dumpGrid(GridCell[][] neighborhood)
          {
               GridCell to_return = null;

               //The j-then-i nesting here is not a bug.
               for(int j=0; j<neighborhood[0].length; j++)
               {
                    for(int i=0; i<neighborhood.length; i++)
                         switch(neighborhood[i][j].contents)
                         {
                         case EMPTY: System.out.print("-");
                              break;
                         case BLOCKED: System.out.print("X");
                              break;
                         case SELF: System.out.print("@");
                              to_return = neighborhood[i][j];
                              break;
                         case ALLY: System.out.print("A");
                              break;
                         case ENEMY: System.out.print("E");
                              break;
                         case WALL: System.out.print("#");
                              break;
                         case FORT:
                              switch(neighborhood[i][j].fort_orientation)
                              {
                              case UP: System.out.print("^");
                                   break;
                              case DOWN: System.out.print("V");
                                   break;
                              case LEFT: System.out.print("<");
                                   break;
                              case RIGHT: System.out.print(">");
                                   break;
                              }
                              break;
                         case CAPSULE: System.out.print("C");
                              break;
                         }
                    System.out.println();
               }

               if(to_return!=null)
                    System.out.println("Position: ["+to_return.x_coord+"]["+to_return.y_coord+"]");
               return to_return;
          }

     public void act(WorldAPI api, Robot_Status status, byte[][] received_radio)
          {
               int x_coord = 0, y_coord = 0;
               GridCell[][] neighborhood = api.getVisibleNeighborhood();
               GridCell self = dumpGrid(neighborhood);
               System.out.println("ManualBot at ("+self.x_coord+","+self.y_coord+"):\n? ");
               String command = scanner.next();
               while(!command.equalsIgnoreCase("done"))
               {
                    try
                    {
                         if(command.equalsIgnoreCase("move"))
                         {
                              System.out.print("Direction? ");
                              String dir = scanner.next();
                              System.out.print("Steps? ");
                              int steps = scanner.nextInt();
                              
                              Direction way = Direction.UP;
                              if(dir.equalsIgnoreCase("Up"))
                                   way = Direction.UP;
                              else if(dir.equalsIgnoreCase("Down"))
                                   way = Direction.DOWN;
                              else if(dir.equalsIgnoreCase("Left"))
                                   way = Direction.LEFT;
                              else
                                   way = Direction.RIGHT;

                              api.move(steps,way);
                         }
                         else if(command.equalsIgnoreCase("attack"))
                         {
                              System.out.print("Type? ");
                              String typ = scanner.next();

                              System.out.print("Location x y? ");
                              int x_coord = scanner.nextInt();
                              int y_coord = scanner.nextInt();

                              System.out.print("Power? ");
                              int power = scanner.nextInt();

                              AttackResult result;
                              if(typ.equalsIgnoreCase("melee"))
                                   result = meleeAttack(power,neighborhood[x_coord - neighborhood[0][0].x_coord][y_coord - neighborhood[0][0].y_coord]);
                              else if(typ.equalsIgnoreCase("ranged"))
                                   result = rangedAttack(power,neighborhood[x_coord - neighborhood[0][0].x_coord][y_coord - neighborhood[0][0].y_coord]);
                         }
                    }
                    catch(RoboSim.RoboSimExecutionException e)
                    {
                         System.out.println(e.getMessage());
                    }

                    //Print out new grid
                    dumpGrid(neighborhood = api.getVisibleNeighborhood());

                    //Get next command ("?" for interface mimics ed)
                    System.out.print("? ");
                    command = scanner.next();
               }
          }
}
