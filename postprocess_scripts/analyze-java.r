# This file is part of SmartDiet.
# 
# Copyright (C) 2011, Aki Saarinen.
# 
# SmartDiet was developed in affiliation with Aalto University School 
# of Science, Department of Computer Science and Engineering. For
# more information about the department, see <http://cse.aalto.fi/>.
# 
# SmartDiet is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
# 
# SmartDiet is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with SmartDiet.  If not, see <http://www.gnu.org/licenses/>.

fm <- function(x, key) {
  methodAll = x$interesting_method_count
  cat(key)
  methodNum = x[,sprintf("%s_method_count", key)]
  (methodNum / methodAll)
}

formatperc <- function(p) {
  sprintf("%.1f \\%%", p * 100.0)
}

x <- read.csv("methods.csv", header=TRUE, row.names = NULL)
ic <- x$interesting_class_count

stats <- data.frame(
    application = x$application,    
    interesting_methods = x$interesting_method_count,
    not_strict = fm(x, "not_strict_serializable"),
    not_relaxed = fm(x, "not_relaxed_serializable"),
    hard = fm(x, "hard_constrained"),
    state = fm(x, "state_constrained")
  )

cat("\nApplication & Methods & Not directly serializable & Not easily serializable & Has hard constraints & Has state synchronization constraints \\\\\n")
cat("\\hline\n")
for (i in 1:(nrow(stats))) {
  a <- stats[i,]
  name <- a$application
  cat(sprintf("%s & %s & %s & %s & %s & %s \\\\\n", 
    a$application,
    a$interesting_methods,
    formatperc(a$not_strict),
    formatperc(a$not_relaxed),
    formatperc(a$hard),
    formatperc(a$state)
    ))
}


printstats <- function(s, field, desc) {
  cat(sprintf("%s & ", desc))
  cat(sprintf("%.2f\\%% & ", 100.0*median(s[,field])))
  cat(sprintf("%.2f\\%% & ", 100.0*min(s[,field])))
  cat(sprintf("%.2f\\%% \\\\\n", 100.0*max(s[,field])))
}
cat("\n\nStatistic & Median & Min & Max \\\\\n")
cat("\\hline \n")
cat(sprintf("Number of methods in app & %d & %d & %d \\\\\n",
  median(stats$interesting_methods),
  min(stats$interesting_methods),
  max(stats$interesting_methods)))
 
printstats(stats, "not_strict", "Not strictly serializable")
printstats(stats, "not_relaxed", "Not serializable with simple operations")
printstats(stats, "hard", "Has hard constraints")
printstats(stats, "state", "Has state constraints")
