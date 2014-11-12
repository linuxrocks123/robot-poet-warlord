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

     public void act(WorldAPI api, Robot_Status status, byte[][] received_radio)
          {
               //TODO: everything...
               System.out.print("***Move***\nDirection? ");
               String dir = scanner.nextLine();
               System.out.print("Steps? ");
               int steps = 0;
               try
               {
                    steps = Integer.parseInt(scanner.nextLine());
               }
               catch(NumberFormatException e)
               {
                    System.out.println(e.getMessage());
               }

               Direction direction = Direction.UP;
               if(dir.equalsIgnoreCase("Up"))
                    direction = Direction.UP;
               else if(dir.equalsIgnoreCase("Down"))
                    direction = Direction.DOWN;
               else if(dir.equalsIgnoreCase("Right"))
                    direction = Direction.RIGHT;
               else
                    direction = Direction.LEFT;

               try
               {
                    api.move(steps,direction);
               }
               catch(RoboSim.RoboSimExecutionException e)
               {
                    System.out.println(e.getMessage());
               }
          }
}
