/**This class provides methods that duplicate the functionality of ArrayList
 * but without the space and lookup overhead of double indirection.  It only
 * supports arrays of ints.  Premature optimization?  It's actually here
 * because I didn't know 1301 covered ArrayList, so shut up.<br><br>
 * Note that this class is wildly time-inefficient and unsuitable for use
 * with anything but the smallest arrays.  Unlike ArrayList, which uses
 * exponential doubling of allocated arrays, these methods specify a system
 * in which the array is reallocated <i>at every addition and deletion</i>.
 * This is necessary because the length field has to always be exact so that
 * students can rely on it.
 */
public class ArrayUtility
{
     /**Add element to array
      * @param current array to duplicate and add element
      * @param elem element to add
      * @return new array 1 element larger with elem appended
      */
     public static int[] addElement(int[] current, int elem)
     {
          int[] to_return = new int[current.length+1];
          for(int i=0; i<current.length; i++)
               to_return[i]=current[i];
          to_return[to_return.length-1]=elem;
          return to_return;
     }

     /**Delete array element
      * @param current array with element to delete
      * @param index index of element to delete
      * @return new array with element at index deleted
      */
     public static int[] deleteElement(int[] current, int index)
     {
          int[] to_return = new int[current.length-1];
          for(int i=0,j=0; i<current.length; i++,j++)
               if(i==index)
                    j--;
               else
                    to_return[j] = current[i];
          return to_return;
     }

     /**Linear search:
      * Find first occurrence of value in array, return index
      * @param current array
      * @param value value to find
      * @return index if found, -1 if not
      */
     public static int linearSearch(int[] current, int value)
          {
               for(int i=0; i<current.length; i++)
                    if(current[i]==value)
                         return i;
               return -1;
          }
}
