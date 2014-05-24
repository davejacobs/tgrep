Background
==========

`tgrep` is a commandline utility meant to search 100 GB log files.

### Prerequisites ###

`tgrep` is written in Clojure and, as such, requires a JVM to be installed on your computer. Leiningen is required to install Clojure and library dependencies.

### Running ###

To run:

    lein search 00:05
    lein search -f logfile 00:05
    lein search -f logfile 00:05-00:10
    lein search -f logfile 00:05:01-00:10
    lein search -f logfile 00:05:01-00:10:01

### Edge cases ###

There were several different potential edge cases and bugs that could've cropped up with this utility. These are mainly the product of:

1. The required flexibility of the input value precision (the user can
   supply times of arbitrary precision to the program, and the
   program has to decide how precise the user wants to be
   and allow a range of acceptable values)
2. The required flexibility of the number of input values (the user
   can supply one time or a range)
3. The specification that the log encompasses slightly more than
   a 24 hour timepoint. This means that a specified date interval range
   might appear twice in a log file.

I've tried to remedy edge cases up front by normalizing all incoming
dates and immediately turning them into intervals. Any precise values
simply turn into one boundary for the time interval we're looking at,
and if we're only looking for one timestamp, I simply make an interval
out of two identical date values.

To combat (3), I simply decided that the first range within the valid
bounds of the log file would be the target range.

### Performance ###

This code runs nearly as fast as vanilla Java, and it will self-optimize
the more you run it. One drawback about using the JVM is that the VM
must load before the script can execute. A persistent JVM can solves
this problem. To give you an indication of the actual run time of my
script, I've included an "Elapsed time" tag after the script runs.
