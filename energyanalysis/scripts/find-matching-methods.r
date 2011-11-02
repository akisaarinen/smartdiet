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

# Read timestamp data
read_start_time <- function(f) 
{
  ts <- read.table(sprintf("%s.timestamp", f), header=TRUE)
  return(trunc(ts[1, 'timestamp'] / 1000))
}

# Read method trace, offsets from start_time
read_method_trace <- function(filename, start_time) 
{
  print(sprintf("Reading method trace from '%s'",filename))
  M <- read.table(sprintf("%s.abs",filename), header=TRUE)
  M <- cbind(M[1:2], M[3] / 1000 - start_time, M[4])
  return(M)
}

# Read network, offsets from start_time
read_network_trace <- function(filename, start_time)
{
  print(sprintf("Reading network trace from '%s'",filename))
  N_data <- read.table(sprintf("%s.nw",filename), header=TRUE)
  N_data <- cbind(N_data[1:1] - start_time, N_data[2:ncol(N_data)])
  N_indices <- matrix(data=0:(nrow(N_data)-1), nrow=nrow(N_data), ncol=1, dimnames=list(NULL, list('packet_index')))
  N <- cbind(N_data, N_indices)
  return(N)
}

# Plotting together
plotcalls <- function (M, N, p_start_time, threads, nmax=20)
{
  nx <- N[1:nmax,1]
  ny <- N[1:nmax,2]
  
  #View(M[3000:4000,]);
  #View(N);
  
  nx_in <- N[N$direction == 2,1]
  ny_in <- N[N$direction == 2,2]
  
  
  nx_out <- N[N$direction == 1,1]
  ny_out <- N[N$direction == 1,2]

  
  x_start = 10500
  x_end = 11000
  cex = 10/(par("cin")[1]*25.4)
  
  textcex = 7/(par("cin")[1]*25.4)
  
  plot(nx_in, ny_in, xlab='Program execution time (milliseconds)', ylab='', yaxt='n', 
    col='red', pch=1, lwd=2,
    cex=cex,
    xlim=c(x_start,x_end),
    ylim=c(45000, 75000)
    )
    
  points(nx_out,ny_out, 
    col='red', pch=2, lwd=2,
    cex=cex)
    
  #title("TCP packets and network-related method calls")
  legend(x_start, 75000, col='red', pch=1, 'Incoming network packets', cex=textcex); 
  legend(x_start, 69000, col='red', pch=2, 'Outgoing network packets', cex=textcex);
  legend(x_start + (x_end - x_start)/2, 75000, col='blue', pch=3, 'Network-related method calls', cex=textcex);
  
  # Threads
  offset <- 0
  for (t in threads) {
    M11 <- M[M$thread==t,3:2]
    mx <- M11[,1]
    my <- array(50000 + offset*2500, c(length(mx)))
  
    points(mx,my,
      col='blue', pch=3, lwd=1,
      cex=cex)
    offset <- offset + 1
  }
}

get_tseries <- function(x, start, stop, window, overlap) 
{
  count <- (stop-start)/(window-overlap)
  s <- array(0, count)
  xpos <- 0
  spos <- 0
  while (xpos < stop) {
    x_in_win <- x[x >= xpos & x <= (xpos + window)]
    s[spos] <- length(x_in_win)
    xpos <- xpos + (window - overlap)
    spos <- spos + 1
  }
  return(s)
}

find_matching_threads <- function(N, M, conn_id, threads, 
                                  window = 10, overlap = 5, max_lag_ms = 30, 
                                  correlation_tolerance_value = 0.10) 
{                                 
  x <- N[N$conn_id==conn_id,]$timestamp
  t_start <- min(x)
  t_stop <- max(x)
  max_lag <- max_lag_ms / (window-overlap)
  
  tx <- get_tseries(x, t_start, t_stop, window, overlap)
  
  c_max <- -Inf
  t_max <- -1
  l_max <- 0
  
  found_t <- list()
  found_c <- list()
  found_l <- list()
  
  for (t in threads) {
    y <- M[M$thread==t,]$ts
    ty <- get_tseries(y, t_start, t_stop, window, overlap)
    if (max(ty) > 0) {
      corr <- ccf(tx,ty, lag.max = max_lag, plot=FALSE)
      
      c_value <- max(corr$acf)      
      c_index <- which(corr$acf == c_value)[1]
      c_lag <- corr$lag[c_index]
      
      if (c_value > correlation_tolerance_value) {
        found_t <- c(found_t, list(t))
        found_c <- c(found_c, list(c_value))
        found_l <- c(found_l, list(c_lag))
      }
      
      if (c_value > c_max) {        
        t_max <- t
        c_max <- c_value
        l_max <- c_lag
      }
    } else {
      c_value <- 0
    }
    #print(sprintf("Correlation for thread %d and connection %d is %f", t, conn_id, c_value))    
  }  
  results <- data.frame(thread=unlist(found_t), value=unlist(found_c), lag=unlist(found_l))
  return(results)
}

#Cpu <- read.table('calc.cpu', header=TRUE)
#SummedCpu <- cbind(Cpu[1:1], Cpu[3:3] + Cpu[4:4] + Cpu[5:5])
#plot(SummedCpu)
#lines(SummedCpu)

analyze <- function(M,A,N,base_name,start_time) {    
  print(sprintf("Analyzing data for dataset '%s'", base_name))
  
  # Get unique items
  connections <- sort(unique(N$conn_id))
  threads <- sort(unique(M$thread))
    
  # Plot
  plotcalls(M,N,p_start_time, threads)
  # TODO: Remove
  return
  
  print("Finding matches for connections")

  # Find matching method for all items
  matches <- array(dim=c(nrow(N), 7),
    dimnames=list(NULL, list('timestamp', 'conn_id', 'thread', 'method', 'code', 'packet_size', 'packet_index')))
  match_index <- 1
  
  # Loop each connection and try to find which thread would be
  # most responsible (for now, let's say it would be only one 
  # even though this is not really always the case).
  for (conn_id in connections) {
    #results <- find_matching_threads(N, M, conn_id, threads)
    results <<- find_matching_threads(N, M, conn_id, threads)
 
    if (length(results) > 0) {
      # Assign each packet to some particular method
      P <- N[N$conn_id == conn_id,]      
      print(sprintf("Matching threads for connection %d (%d packets): %s (correlations: %s)", 
        conn_id,
        nrow(P),
        paste(results$thread, sep="", collapse=", "),
        paste(sprintf("%.2f",results$value), sep="", collapse=", ")
        ))
      for (p in 1:nrow(P)) {            
        ts <- P$timestamp[p]
#         # Find last method that was entered but has not exited until the packet was encountered
#         # Might work well for blocking reads?
#         enters <- which(M$thread == results$thread & M$ts <= ts & M$code == 'ent')
#         last_entered_id <- -1
#         for (ent in rev(enters)) {
#           method <- M$method[ent]
#           enter_ts <- M$ts[ent]          
#           
#           # problem: different threads mix up
#           ents_after <- which(M$thread == results$thread & M$ts >= enter_ts & M$code == 'ent' & M$method == method)
#           xits_after <- which(M$thread == results$thread & M$ts >= enter_ts & M$code == 'xit' & M$method == method)
#           
#           if (length(ents_after) >= length(xits_after)) {
#             last_entered_id = ent
#             break
#           }
#         }
#         print(sprintf("[packet-%d] @%d, dir %d (%s), %s @%f", 
#           p, 
#           P[p, 'timestamp'],          
#           P[p, 'direction'], 
#           P[p, 'event'], 
#           M$method[last_entered_id],  
#           M$ts[last_entered_id]
#           ))
        
#         # Another approach: Find methods with small enough time diff and assign a point for each
#         diffs <- M[,'ts'] - ts
#         small_enough <- which(diffs <= 20 & diffs >= -20)            
#         for (i in small_enough) {
#           print(sprintf("%d [%02d] Diff: %f, method: %s (%s)", conn_id, M[i, 'thread'], diffs[i], M[i, 'method'], M[i, 'code']))
#         }




        # Nearest time and that's it
        diffs <- abs(M$ts - ts)
        value <- min(diffs)
        i <- which(diffs == value)[1]        
        
        # Packet index
        packet_index <- P[p, 'packet_index']
        
        # Get packet size if available
        if (!is.null(P[p, 'size'])) {          
          packet_size <- P[p, 'size']
        } else {
          packet_size <- 0
        }
        
        # Print info
        print(sprintf("%d/%d (%d bytes) [thread %02d] => (%d) Diff: %f, method: %s (%s)", conn_id, packet_index, packet_size, M[i, 'thread'], i, diffs[i], M[i, 'method'], M[i, 'code']))        
        
        # Assign to matches        
        matches[match_index, 'timestamp'] <- M[i, 'ts']
        matches[match_index, 'conn_id'] <- conn_id
        matches[match_index, 'thread'] <- M[i, 'thread']
        matches[match_index, 'method'] <- as.character(M[i, 'method'])
        matches[match_index, 'code'] <- as.character(M[i, 'code'])
        matches[match_index, 'packet_index'] <- packet_index
        matches[match_index, 'packet_size'] <- packet_size
                
        match_index <- match_index + 1
      }
    } else {
        print(sprintf("No matching thread(s) for connection %d found", conn_id))
    }
  }
  #View(matches)
  
  # Write matched methods
  matched_method_filename = sprintf("%s-matched_methods.data", base_name)  
  print(sprintf("Writing packet match results to %s", matched_method_filename))
  write.table(matches, matched_method_filename, sep=',')
  
  # Map matched methods to all methods and increment counts
  print(sprintf("Mapping network packets to all packets, network match count: %d", nrow(matches)))
  C <- matrix(data=0, nrow=nrow(A), ncol=3)
  colnames(C) <- list('packet_count', 'packet_size', 'packet_indices')
  
  # Clear list of indices
  for (c in 1:nrow(C)) {
    C[c, 'packet_indices'] <- ""
  }
  
  for (m in 1:nrow(matches)) {
    method <- which(A$ts == matches[m, 'timestamp'] & 
                    A$method == matches[m, 'method'] & 
                    A$code == matches[m, 'code'] & 
                    A$thread == matches[m, 'thread'])[1]    
    if (!is.na(method)) {                    
      C[method, 'packet_count'] <- as.integer(C[method, 'packet_count']) + 1
      matched_packet_size <- as.integer(matches[m, 'packet_size'])    
      C[method, 'packet_size'] <- as.integer(C[method, 'packet_size']) + matched_packet_size
      
      # Accumulate indices list
      matched_packet_index <- matches[m,'packet_index']
      indices <- C[method, 'packet_indices']
      if (as.character(indices) == "") {
        indices <- as.character(matched_packet_index)
      } else {
        indices <- paste(list(indices, matched_packet_index), collapse=":")    
      }
      C[method, 'packet_indices'] <- indices
      print(sprintf("Processing %d, matched row %d, packet index %d", m, method, as.integer(matched_packet_index)))
      print(C[method,]) 
    } else {
      print(sprintf("Processing %d, no match found, stopping (method trace ended before packet trace)", m))
      break
    }
  }

  # Bind counts to all methods and write
  X <- cbind(A, C)    
  all_methods_filename = sprintf("%s-all_methods.data", base_name)
  print(sprintf("Writing all method data to %s", all_methods_filename))
  write.table(X, all_methods_filename, sep=",")
}

print_analysis <- function(base_name) { 
  start_time <- read_start_time(base_name)
  M <- read_method_trace(sprintf("%s-network", base_name), start_time)
  A <- read_method_trace(base_name, start_time)
  N <- read_network_trace(base_name, start_time)
  analyze(M, A, N, base_name, start_time)
}
x <- commandArgs(trailingOnly = TRUE)
filename <- x[1]
cat ("Analyzing from file ")
cat (filename)
cat ("\n")
print_analysis(filename)
