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

plot_wlan_est <- function(t, color)
{
  x <- (t$est_wlan_mj)
  points(x, pch=20, col=color)
  lines(x, col = color)
}

plot_umts_est <- function(t, color)
{
  x <- (t$est_umts_mj)
  points(x, pch=20, col=color)
  lines(x, col = color)  
}

plot_pt <- function(t, color)
{
  pt <- (t$pt_mj)
  points(pt, pch=21, col=color)
  lines(pt, col = color)  
  
  
  
  x <- (t$packet_timespan_ms)
  points(x, pch=22, col=color)
  lines(x, lty=2, col=color)
}

show_stats <- function(label, tl, tr, field, unit) {
  xl <- tl[,field]
  xr <- tr[,field]
  print("---------")
  print(sprintf('%s:', label))
  print(sprintf('Local:     mean %.2f %s, median %.2f %s, stdev %.2f %s',
    mean(xl),
    unit,
    median(xl),
    unit,
    sd(xl),
    unit))
  print(sprintf('Offloaded: mean %.2f %s, median %.2f %s, stdev %.2f %s',
    mean(xr),
    unit,
    median(xr),
    unit,
    sd(xr),
    unit))   
    
  if (mean(xl) > mean(xr)) {
    print(sprintf("(+) MEAN:   Offloading saves %.0f%% when compared to local",
      100 - (mean(xr) / mean(xl)) * 100))
  } else {
    print(sprintf("(-) MEAN:   Offloading wastes %.0f%% when compared to local",
      ((mean(xr) - mean(xl)) / mean(xl)) * 100))
  }
  
  if (median(xl) > median(xr)) {
    print(sprintf("(+) MEDIAN: Offloading saves %.0f%% when compared to local",
      100 - (median(xr) / median(xl)) * 100))
  } else {
    print(sprintf("(-) MEDIAN: Offloading wastes %.0f%% when compared to local",
      ((median(xr) - median(xl)) / median(xl)) * 100))
  }
}


meas_type <- "3G"
t_tr <- read.csv("tr3g.csv", header=TRUE)
t_tl <- read.csv("tl3g.csv", header=TRUE)

count <- max(nrow(t_tr), nrow(t_tl))
xmax <- count


pt_max <- max(t_tr$pt_mj, 
            t_tl$pt_mj)
umts_max <- max(t_tr$est_umts_mj,
                t_tl$est_umts_mj) 
wlan_max <- max(t_tr$est_wlan_mj,
                t_tl$est_wlan_mj) 
                

if (meas_type == "3G") {
  ymax <- max(pt_max, umts_max) * 1.1
} else {
  ymax <- max(pt_max, wlan_max) * 1.1
}

plot(0,xlim=c(1,xmax),ylim=c(0,ymax),xlab='Reloading attempt index', ylab='Energy (mJ)')
title(sprintf('Energy consumption for %d twitter timeline reloads over %s',
  nrow(t_tr) + nrow(t_tl),
  meas_type))

meas_local_color <- "blue"
meas_remot_color <- "red"
legend(xmax/2, ymax + ymax/40, col=meas_local_color, pch=21, 'Measured (Local)')
legend(1, ymax + ymax/40, col=meas_remot_color, pch=21, 'Measured (Offloaded)')
plot_pt(t_tl, meas_local_color)
plot_pt(t_tr, meas_remot_color)


plot_estimates <- TRUE
if(plot_estimates) {
  est_local_color <- "lightblue"
  est_remot_color <- "pink"
  legend(xmax/2, ymax/14, col=est_local_color, pch=20, 'Network estimate (Local)')
  legend(1,ymax/14, col=est_remot_color, pch=20, 'Network estimate (Offloaded)')
  if (meas_type == "3G") {
    plot_umts_est(t_tl, est_local_color)
    plot_umts_est(t_tr, est_remot_color)
  } else {
    plot_wlan_est(t_tl, est_local_color)
    plot_wlan_est(t_tr, est_remot_color)
  }
}
 
show_stats('Estimate based on network packets (WLAN)', t_tl, t_tr, 'est_wlan_mj', 'mJ')
show_stats('Estimate based on network packets (UMTS)', t_tl, t_tr, 'est_umts_mj', 'mJ')
show_stats('Measured energy (Monsoon)', t_tl, t_tr, 'pt_mj', 'mJ')
show_stats('Time for reloading to complete', t_tl, t_tr, 'packet_timespan_ms', 'ms')
show_stats('Total transfer size', t_tl, t_tr, 'packet_bytes', 'bytes')
show_stats('TCP packet count for reload', t_tl, t_tr, 'packet_count', 'packets')
