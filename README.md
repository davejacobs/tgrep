Background
==========

Tgrep is a commandline utility meant to search log files per Reddit's specifications [0].

Setup
=====

Tgrep is written in Clojure and, as such, requires a JVM and the Clojure language [1] to be installed on your computer. What's more, it manages its project dependencies using Leiningen [2], so lein is required to run the utility, as well. All other libraries are included with the executable.

Ruby is used instead of Bash to actually parse arguments in the executable and then run the Clojure code as a script. Any version of Ruby after 1.8.6 should do.

To run the executable, first give it permission to execute via chmod +x bin/tgrep.

Then run the program (bin/tgrep) according to the Reddit specifications. /log/haproxy.log is the default file, if none other is specified.

Edge cases
==========

There were several different potential edge cases and bugs that could've cropped up with this utility. These are mainly the product of:

  a. The required flexibility of the input value precision (the user can
     supply times of arbitrary precision to the program, and the
     program has to decide how precise the user wants to be
     and allow a range of acceptable values)
  b. The required flexibility of the number of input values (the user
     can supply one time or a range)
  c. The specification that the log encompasses slightly more than
     a 24 hour timepoint. This means that a specified date interval range
     might appear twice in a log file.

I've tried to remedy edge cases up front by normalizing all incoming dates
and immediately turning them into intervals. Any precise values simply turn into one boundary for the time interval we're looking at, and if we're only looking for one timestamp, I simply make an interval out of two identical date values.

To combat c., I simply decided that the first range within the valid bounds of the log file would be the target range.

Performance
===========

One note about performance. This code runs as fast as vanilla Java, and it will self-optimize the more you run it. One drawback about using the JVM is that the VM must load before the script can execute. I've talked with jedberg about this problem, and he said that the JVM load time would not be an issue. To give you an indication of the actual run time of my script, I've included an "Elapsed time" tag after the script runs. To mediate the JVM load time, a nailgun Java server (which gives you a persistent JVM) may also be used.

[0] http://www.reddit.com/r/blog/comments/fjgit/reddit_is_doubling_the_size_of_its_programming/

[1] http://clojure.org/getting_started

[2] https://github.com/technomancy/leiningen
