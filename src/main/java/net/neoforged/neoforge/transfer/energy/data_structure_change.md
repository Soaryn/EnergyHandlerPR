|                   |         int |                 long |
|-------------------|------------:|---------------------:|
| **Lowest Value**  | -2147483648 | -9223372036854775808 |
| **Highest Value** |  2147483647 |  9223372036854775807 |
| **Size**          |     4 bytes |              8 bytes |

## Number boundaries

While most power numbers should lie well below the value of even just a `short`,
there are scenarios where an integer can't quite support the size that is needed.
The biggest problem is that developers tend to want to lean towards the upper bound of what ever their limit is,
so increasing size would likely result in just an inflation of byte size with the exact same problem that we have now.

Let's assume the values that are typically used are not immediately increased dramatically, but instead lowered;
a larger buffer could allow for someone to be able to bulk transfer energy. This argument makes sense; however,
let's assume people push the numbers all the way to the brink again. While the premise of the argument was sound, this
is already true with `int` values now.

The idea of "big number go brr" has never actually been accurate. It is really the delta of your previous value,
not just a number being arbitrarily big. In other words, when you start playing any game that has a energy valued
currency,
you typically have 0 production. After a while you have say 4 energy over time; then later 10 energy. This is usually a
supply and demand problem,
but most players are always seeking the MOST POWER NOW, also known as "optimizing the fun away".

The premise right now, is that there is nothing stopping you from storing a near unlimited amount of energy in what ever
handler you make,
but at some point there is a data structure limit, and it is not immediately apparent that increasing the range would be
beneficial.

## Size

This is likely the more critical one. There are two main components, the first is doubling the byte size is not much of
an issue,so long as we get a benefit out of doing it.In the aforementioned boundary increased, it wouldn't be
necessarily an improvement.  