/**DefenderBot:
 * Robot implementation which just sits there and never moves or attacks.
 * Uses its power to repair itself if it's damaged, then to charge if it's
 * not fully charged, then to defend itself.
 */

public class DefenderBot implements Robot
{
     private Robot_Specs my_specs;

     public Robot_Specs createRobot(WorldAPI api, int skill_points, byte[] message)
          {
               Robot_Specs to_return = new Robot_Specs();
               to_return.attack = 0;
               to_return.power = to_return.charge = skill_points/3;
               to_return.defense = skill_points/3 + skill_points%3;

               //This handles the pathological case where skill_points<3
               if(skill_points<3)
               {
                    to_return.defense = 0;
                    to_return.charge = 1;
                    to_return.power = skill_points - to_return.charge;
               }

               //Keep track of our specs; simulator won't do it for us!
               my_specs = to_return;
               return to_return;
          }

     public void act(WorldAPI api, Robot_Status status, byte[][] received_radio)
          {
               int remaining_power = status.power;
               int remaining_charge = status.charge;

               //Are we damaged
               if(status.health!=my_specs.charge*10)
               {
                    int repair_amount = (my_specs.charge*10 - status.health) * 2;
                    if(repair_amount > status.power)
                         repair_amount = status.power - status.power%2;
                    try
                    {
                         api.repair(repair_amount);
                    }
                    catch(RoboSim.RoboSimExecutionException e)
                    {
                         System.out.println(e.getMessage());
                    }
                    remaining_power-=repair_amount;
                    remaining_charge-=repair_amount;
               }

               /*Next priority is charging ourselves
                 We will automatically charge (charge skill) amount next
                 turn, so limit amount of power we save for charging to the
                 minimum required to get to (charge skill*9).
               */
               if(remaining_charge < my_specs.charge*9)
               {
                    //How much to save?
                    int to_save = my_specs.charge*9 - remaining_charge;
                    if(to_save > remaining_power)
                         to_save = remaining_power;
                    remaining_power-=to_save;
               }

               //Any remaining power we put toward defense
               if(remaining_power > 0)
                    try
                    {
                         api.defend((remaining_power <= my_specs.defense) ? remaining_power : my_specs.defense);
                    }
                    catch(RoboSim.RoboSimExecutionException e)
                    {
                         System.out.println(e.getMessage());
                    }
          }
}
