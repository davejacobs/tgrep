# Background

`tgrep` is a commandline utility meant to search very large (> 100 GB) log files quickly by timestamp.

## Prerequisites

`tgrep` is written in Clojure and, as such, requires:

- A JVM
- Leiningen

## Examples

    lein search 00:05
    lein search -f logfile 00:05
    lein search -f logfile 00:05-00:10
    lein search -f logfile 00:05:01-00:10
    lein search -f logfile 00:05:01-00:10:01

## Edge cases

There are several potential edge cases that came up
parsing logs. These are mainly the product of:

1. The required flexibility of the input value precision (the user can
   supply times of arbitrary precision to the program, and the
   program has to decide how precise the user wants to be
   and allow a range of acceptable values)
2. The required flexibility of the number of input values (the user
   can supply one time or a range)
3. The specification that the log encompasses slightly more than
   a 24 hour timepoint. This means that a specified date interval range
   might appear twice in a log file.

I've tried to remedy edge cases up front by sanitizing incoming
dates and immediately turning them into intervals. Any precise values
simply turn into one boundary for the time interval we're looking at,
and if we're only looking for one timestamp, I simply make an interval
out of two identical date values.

To combat (3), I simply decided that the first range within the valid
bounds of the log file would be the target range.

## Performance

This code runs nearly as fast as vanilla Java. One drawback about using the JVM
is that the VM must load before the script can execute. A persistent JVM can solve
this problem. To give you an indication of the actual run time of the
script, I've included an "Elapsed time" tag after the script runs.

Example data point: For an evenly distributed ~4 GB file, on a 2011 MacBook Air, `tgrep` takes
about 150 ms to find 60 lines.

## TODO

- Define upper and lower performance bounds for common file sizes
- Move time-related queries into a time namespace.
- Port to Node/ClojureScript to eliminate startup time.
- Transform into commandline utility that doesn't use leiningen.
- Make searching as lazy as possible.
- Allow for more flexible inputs.
- Allow for configurable time search formats.
- Don't assume non-repeated times.
