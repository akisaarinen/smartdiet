/* 
 * This file is part of TrafficMonitor.
 * 
 * Copyright (C) 2011, Aalto University School of Science and the 
 *                     original authors.
 * 
 * TrafficMonitor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * TrafficMonitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with TrafficMonitor.  If not, see <http://www.gnu.org/licenses/>.
 */

#ifndef CONNECTIONS_H_
#define CONNECTIONS_H_

#include <linux/ip.h>
#include <net/tcp.h>


/**
 * Connection states
 */
#define NO_CONNECTION		0
#define SYNED				1
#define ACKED				2
#define ESTABLISHED			4
#define THROTTLE_DETECTION	8
#define NORMAL				16
#define PSMT				32
#define CLOSED				64
#define UNKNOWN				128
#define ERROR				256


// The burst transmission stage. Only applis for the burst transmission, meaning the mobile phone initializes the transmission by sending a SYN packet, and after 3 hand-shakes, the server would still send one(or more) 0-length packet before the actual traffic, and we are not interested in those 0-length packets in our burst prediction model

#define BURST_NO_CONNECTION   0  // The initialization stage, when there is no
#define   SYN    1       // When the client has transmitted the SYN packet and waiting for the SYN_ACK
#define   SYN_ACK  2        // When the client has received the SYN_ACK from the server
#define  BURST_ESTABLISHED  3    // When the burst has been established (before the first real burst packet is sent), not means the connection itself
#define BURST_REQUEST 4    // After the three-way handshakes. Before the client sends the request packet for the real burst
#define BURST_START   5    // After the burst request packet has been sent from the client
#define  FIN         6    // When the connection is about to finish


  


/**
 * Choke States
 *
 * Used during 'Throttle Detection' state.
 * When the connection enters into 'Throttle
 * Detection' state, the choke_state is
 * initialized as PRE_CHOKE.
 *
 * - Normal Flow rates are calculated during
 * PRE_CHOKE and POST_CHOKE.
 *
 * - New flow rate i.e. the flowrate of the
 * bursty traffic is calculated during
 * OPEN_ACK.
 *
 * - During NO_OP, flow rate is not calculated and
 * is the final state during 'Throttle Detection'
 * state
 */
#define NO_OP			0	// No operation, Do not choke and
							// Do NOT calculate any flow rate
#define CALC_FLOWRATE	1	// Calculation of flow rate before entering throttle detection
#define PRE_CHOKE		2	// Before sending a Closed ACK
#define POST_CHOKE		3	// After sending a Closed ACK
#define OPEN_ACK		4	// Open ACK, similar to NO_OP,
							// only difference is that timer
							// is also started during throttle
							// detection
/*
 * PSMT Choke States
 *
 * NO_OP is already defined
 * which will be used for PSMT Choke
 * state also
 */
#define INITIAL_CHOKE			1
#define INITIAL_WAIT			2
#define ADVERTISE_WINDOW_SIZE	3
#define CHOKE					4
#define RECEIVE_PACKETS			5

/*
 * Signals used by the following components:
 * 1. Hooks:	regarding the states of the connections
 * 				i.e Awake and Idle
 *
 * 2. Scheduler:	signals are sent to the emulated
 * 				 	WNIC
 *
 * 3. WNI Control:	signals sent from the Scheduler to
 * 					the stubs for WNI control.
 *
 * Also emulation states that are used by hooks
 * to account for the WNIC transition times.
 */
#define NOT_IDLE				0
#define IDLE					1
//#define EMULATE_SINGLE_PACKET	2

/*
 * Threshold for getting into 'Throttle Detection' state
 */
#define PACKET_COUNT_THRESHOLD	50
#define DATA_THRESHOLD			51200		// 50 KBs

/*
 * Default Maximum Segment Size
 */
#define DEFAULT_MSS_VALUE		1452

/*
 * PSM Throttling will not be enabled if the
 * ratio of the detected flow rate and the normal
 * flow rate is less than the BANDWIDTH_RATIO
 */
#define BANDWIDTH_RATIO			2

/*
 * The choke factor multiplied by RTT is the duration
 * for which the traffic is choked to determine any
 * unused bandwidth
 */
#define CHOKE_FACTOR			3

/*
 * Transition times used by the Wifi Device
 *
 * The transition time from sleep to wake is used
 * by the local out hook to wait for the appropriate
 * amount of time before sending an acknowledgment
 * i.e if the underlying device is already in sleep
 * state.
 *
 * The following values also help in:
 *
 * Emulating the WNIC
 * ------------------
 *
 * If the WNIC is being emulated, the following
 * values should not be ZERO. Transition times
 * from sleep to wake and vice versa should be
 * set (in milliseconds)
 */
#define TRANSITION_TIME_SLEEP_TO_WAKE 3 // should not be zero even if the WNIC is not being emulated.
#define TRANSITION_TIME_WAKE_TO_SLEEP 0

/*
 * Estimated time required to send out a packet.
 * So if the device is in sleep mode it will take the
 * TRANSITION_TIME_SLEEP_TO_WAKE to wake up
 * and then an extra SEND_SINGLE_PACKET_TIME to send the
 * packet.
 * (in milliseconds)
 */
#define SEND_SINGLE_PACKET_TIME 1

/*
 * The safest way to wake up from the sleep state is atleast an
 * RTT minus the RTT Variance. The Agression val acts as a divisor for
 * RTT Variance. Higher the aggression value, less will be the RTT Variance.
 */
#define AGGRESSION_VALUE_N 2
#define AGGRESSION_VALUE_D 4



/*
 * The throughput during PSM Throttling needs
 * to remain between the following two thresholds
 *
 * The thresholds should be dynamically changed to
 * achieve optimum performance. The functionality
 * has not been added yet!
 * todo: If the throughput at the begining of the
 * connection is incorrectly detected, the algorithm
 * will fail. Need to lower the min threshold as a
 * temporary solution..
 */

#define INITIAL_MAX_PSMT_THROUGHPUT 97;
#define INITIAL_MIN_PSMT_THROUGHPUT 95;

/*
 *  wait for 10 secs (10,000 millisecs)
 *  before connection enters throttle
 *  detection mode
 */
const unsigned int playout_buffer_wait	= 5000;
const unsigned int calc_flowrate_wait	= 5000;

/*
 * Condition for enabling PSM Throttling
 *
 * Flow Rate (fr)
 * Bandwidth (bw)
 * Throttle Detection (td)
 */
#define ENABLE_PSM_THROTTLING(fr_normal, fr_td, bw_ratio)  1//(fr_td >= bw_ratio*fr_normal)

/*
 * Macro to find out the Window Size to Advertise, during
 * PSM Throttling.
 *
 * The window size is calculated from the total data arrived during
 * 'Throttling Detection' and the srtt. Basically, it calculates the
 * data expected in a shorter time (i.e. the smoothed round trip
 * time or SRTT) than the burst time. For more information about burst
 * time, see the documentation in the constate struct. todo: rename constate?
 */
//#define WINDOW_SIZE(td_data, burst_time, srtt) ((td_data * srtt)/ (BURST_SIZE_RATIO*burst_time))
//#define WIN_SIZE(flowrate_normal, rtt) ( (flowrate_normal*rtt)/1000)

unsigned long test_identifier = 0;

/*
 * Connection state names that appear in the
 * logs.
 */
static char * state_name[] =
  {	"No Connection     ",
    "SYN Sent          ",
    "SYN Acknowledged  ",
    "Established       ",
    "Throttle Detection",
    "Normal Operation  ",
    "PSM Throttling    ",
    "Closed            ",
    "Unknown           ",
    "Error             " };

/*
 * Choke State names that appear in the logs
 * Keeping them short so that more info
 * can be dumped on the screen
 */
static char * choke_state_name[] =
  {	"-   ",	// No Operation
    "C FR", // Calc flow rate
    "PRE ", // Pre Choke
    "POST", // Post Choke
    "OPEN"};// Open ACK

/*
 * PSM Throttling State names that appear in the logs
 */
static char * psmt_state_name[] =
  {	" -         ",		// No Operation
    "INIT CHOKE ",		// Initial Choke
    "INIT WAIT  ", 		// Initial Wait
    "ADV WINSIZE",		// Advertise Window
    "CHOKE      ",
    "RCV PKTS   "};		// Receive Packets


/*
 * Total sleep time is calculated by the
 * scheduler function.
 */
static unsigned int total_sleep_time = 0;


/**
 * Pointer to the head of list of all
 * connections. Whenever this struct is used, it
 * should be locked according to the function.
 */
static struct constate *all_connections_head;
/**
 * SpinLock for the Connection list
 *
 * This lock will be used when any change to the
 * structure of the list is required and not the
 * individual nodes i.e. when changing the 'next'
 * elemenent of a connection.For individual nodes,
 * each node carries it's own spinlock lock.
 *
 * Initialized in the module init
 *
 * todo: Need to evaluate .. do we really want this?
 */
spinlock_t all_connections_spinlock;



struct constate {
  /*
   * The connection id is always the port at the
   * client side. Since client must NOT take the
   * role of a server, hence this id will always
   * be a big number i.e. > 1024 and is unique to
   * the client (atleast for the time being i.e.
   * tests can be conducted during this time)
   *
   */
  u_int32_t connection_id;

  /*
   * TCP Fields
   */
  u_int32_t src_port;
  u_int32_t dst_port;
  u_int16_t window_size;
  u_int16_t previous_window_size;  // The place to store the window size before chocking
  u_long ack_number;
  u_long seq_number;

  /*
   * Receiving Window Scale
   */
  u_int16_t window_scale;

  
  /*
   * Maximum Segment Size
   *
   * This value is cordinated during the TCP
   * Handshake
   */
  u_int32_t tcpi_rcv_mss;

  /*
   * IP Source and Destination Addresses
   */
  u_int32_t src_addr;
  u_int32_t dst_addr;

  /*
   * Round Trip Times
   */
  u_int32_t srtt;		/* Smoothed Round Trip Time 	-	millisecs*/
  u_int32_t rcv_rtt;	/* Receiver Side RTT Estimation -	millisecs*/

  u_int32_t rtt;		/* Round Trip Time				-	nanosecs */
  u_int32_t tcpi_rcv_rtt;		/* Round Trip Time 		-	nanosecs*/
  u_int32_t rtt_var;		/* Round Trip Time 			-	nanosecs*/


  /**
   * State of the connection.
   *
   * Can be one of the above defined states i.e.
   *  - TCP Handshake states such as SYN, SYN-ACK, ESTABLISHED
   *  - Throttle Detection
   *  - PSM Throttling
   *  - Closed etc
   *
   *  Keep it 16 bit just in case we need extra states.
   *
   */
  u_int16_t state;


  int burst_stage;

  int direction;  // To indicate direction of the traffic flow. 0 means this is a download flow, 1 means upload flow.

  u_int32_t timeStamp; // The timestamp of current burst packet in microseconds

  
  // The time stamp of last valid burst packet, used in prediction.
  //  u_int16_t previousTimeStamp;
  

  /*
   * Choke State, defined above
   *
   * Can be one of the following three states:
   * - No Operation, Don't Choke
   * - Pre-Choke
   * - Post-Choke
   */
  u_int8_t choke_state;
  bool chocked;  // to indicate if the window has already been set to 0 (chocked)
  

  /*
   * PSMT state
   */
  u_int8_t psmt_state;

  /*
   * Idle State
   *
   * If the connection can be put to sleep,
   * the idle_state will be 1, else it will
   * be 0. This is possible in the PSM
   * Throttling stage.
   *
   * However it is possible to send outgoing
   * packets during idle state, since the device
   * can come out of the sleep state any time
   * to send them. However, incoming packets will
   * be dropped.
   */
  u_int8_t idle_state;




  /**
   * Flow Rate - Bytes/Sec
   * The values are not accurate since 'int' is
   * used for calculation. However, we don't need
   * accurate values.. We only need to compared
   * the flow rated pre and post choking in the
   * 'Throttle Detection' state.
   */
  u_int32_t flow_rate_normal; // Before Throttle Detection
  u_int32_t flow_rate_td;		// During Throttle Detection
  u_int32_t flow_rate_psmt;		// During PSM Throttling
  u_int32_t flow_rate_inst;		// During PSM Throttling, instantaneous


  u_int32_t data_arrived_td;		// in bytes, used during Throttle Detection
  u_int32_t data_arrived_burst;	// in bytes, used to store total payload in a burst
  u_int32_t temp_data_arrived;	// in bytes, used to store total bytes arrived (after buffer playout)


  u_int32_t psmt_window_size;	// in bytes, packets expected to arrive, NOT scaled
  u_int32_t psmt_window_size_min;	// in bytes, packets expected to arrive, NOT scaled



  /*
   * Only data packets
   *
   * todo: explain when is the value refreshed.
   *
   */
  u_int32_t packet_count_td;
  u_int32_t total_packet_count;

  /*
   * Burst Time
   *
   * This is the time period when the
   * bursty traffic from the server is
   * expected during Throttle Detection
   *
   * In our case it is 2*RTT
   */
  u_int32_t burst_time;

  /*
   * Connection start time in jiffies
   *
   * Used to calculated flow_rate_normal
   * or flow rate when the connection is
   * established.
   */
  u_int32_t connection_start_time;

  u_int32_t td_start_time;

  u_int32_t psmt_start_time;

  /*
   * Total time for which the connection, in psmt
   * state has remained active. This value is
   * compared with the idle time to see the percentage
   * of time saved.
   * Only updated in the scheduler.
   *
   * In Jiffies
   */
  u_int32_t psmt_time_lapsed;

  /*
   * Time for which connection has remained active.
   * In Jiffies
   */
  u_int32_t connection_time_lapsed;


  /*
   * Calculation of the the Total idle time predicted
   * and temp timestamp used to calculate the total
   * idle time.
   * In milliseconds
   */
  u_int32_t total_idle_time;

  /*
   * Temporary timestamp used to sum up idle periods
   * within the scheduler.
   * In Jiffies.
   */
  u_int32_t sleep_timestamp;
  unsigned long wake_timestamp; 	// set in the modify_sleep_timer() function
									// The value is used by the scheduler to see
									// when the connection will wake up.



  u_int8_t max_psmt_throughput;
  u_int8_t min_psmt_throughput;


  /*
   * General Connection timer: used to wait for predefined
   * period of time before calling the timer_function(),
   * in our case it is the wait time in the 'Established'
   * state before moving to the 'Throttle Detection' state
   *
   */
  struct timer_list timer;

  /*
   * Sleep and Wake timers
   *
   * Sleep timer: Used to switch from awake to sleep modes
   * of the underlying wireless device
   * Wake timer: Used to switch to awake mode when the device
   * is (supposedly) sleeping.
   *
   * IMPORTANT : The sleep timer should only be modified using the
   * modify_sleep_timer() function which also updates the
   * wake_timestamp, used by the scheduler to check if any sleeping
   * connection is about to wake up or not.
   *
   *
   * Need for separate timers:
   * Can not use a single timer because the sleep timer
   * might be in use when we need wake functionality.
   *
   * By default, the sleep timer is being used in the local out
   * hook right after the window is advertised in the
   * ADVERTISE_WINSIZE state in PSMT.
   *
   * The wake timer is used when the device is sleeping after
   * having received a packet burst and needs to wake up just to
   * send an acknowledgment.
   *
   */
  struct timer_list sleep_timer; // should be modified using modify_sleep_timer()
  struct timer_list wake_timer;


  rwlock_t connection_rwlock; // readwrite lock

  /*
   * Test Fields.. can be removed
   */
  struct task_struct *task;

  struct constate* next; /* pointer to the next connection */

};

static inline void add_connection(struct constate** arg_con,struct iphdr* arg_ip, struct tcphdr* arg_tcp, u_int32_t arg_con_id,u_int8_t state); /*Append a connection at the end of the list*/
static inline void display_connections_info(struct constate** arg_con);
static inline char * get_current_state_name(u_int16_t state);
static inline void timer_function(unsigned long data);
static inline void sleep_timer_function(unsigned long data);
static inline void wake_timer_function(unsigned long data);
static inline struct constate * get_connection(struct constate** arg_con,u_int16_t arg_port_id);

static inline void update_rtt(struct constate* node,struct sk_buff* conskb); // function that decides the method to fetch rtt
static inline u_int8_t refresh_rtt(struct constate* node, struct sk_buff* conskb);
static inline u_int8_t update_tcpi_rtt(struct constate* node, struct sk_buff* conskb); // obsolete

static inline u_int8_t update_tcpi_rcv_mss(struct constate* node, struct sk_buff* conskb);
static inline void update_flow_rate_normal(struct constate* node);
static inline void update_flow_rate_td(struct constate* node);
static inline void update_flow_rate_psmt(struct constate* node);
static inline u_int32_t calculate_window_size(u_int32_t flowrate_normal , u_int32_t rtt);
static inline void reduce_window_size(struct constate * connection);

static inline void scheduler(struct constate * node, int idle_state, char * log_message);

static inline void delete_all(struct constate** arg_con); /* Delete all the connection*/
static inline int delete_any(struct constate** arg_con, u_int16_t arg_port_id); /* Delete a specific connection from the poll*/
static inline void dump_constate(struct constate** arg_con); /* Print all the connections */
static inline int count(struct constate** arg_con); /* Delete all the connection*/

#endif


/**
 * Append a new connection at the end to the list of connections
 * - Initial version by Shathil
 *
 * Modifications made by Ahmad
 * - Separate connection id and source port
 * - State flags
 * - Timer
 * - Read Write SpinLocks
 */

static inline void add_connection(struct constate** arg_con, struct iphdr* arg_ip, struct tcphdr* arg_tcp, u_int32_t arg_con_id, u_int8_t state) {

  struct constate *node, *temp = *arg_con;
  if (*arg_con == NULL) {
    temp = kmalloc(sizeof(struct constate), GFP_KERNEL);

    /**
     * Initialize Read Write Spin Lock
     */
    rwlock_init( &temp->connection_rwlock);

    temp->connection_id = arg_con_id;
    temp->src_port = ntohs(arg_tcp->source);
    temp->dst_port = ntohs(arg_tcp->dest);
    temp->window_size = ntohs(arg_tcp->window);
    temp->previous_window_size = temp->window_size;

    temp->window_scale = 0;
    temp->tcpi_rcv_mss = DEFAULT_MSS_VALUE;

    temp->seq_number = ntohs(arg_tcp->seq);
    temp->ack_number = ntohs(arg_tcp->ack_seq);

    temp->src_addr = ntohl(arg_ip->saddr);
    temp->dst_addr = ntohl(arg_ip->daddr);

    temp->srtt = 0;
    temp->rcv_rtt = 0;
    temp->rtt = 0;
    temp->tcpi_rcv_rtt = 0;
    temp->rtt_var = 0;


    temp->state = state;
    temp->choke_state = NO_OP;
    temp->psmt_state = NO_OP;
    temp->idle_state = NOT_IDLE; // by default the connection is active

    temp->flow_rate_normal = 0;
    temp->flow_rate_td = 0;
    temp->flow_rate_psmt = 0;
    temp->flow_rate_inst = 0;

    temp->data_arrived_td = 0;
    temp->data_arrived_burst = 0;
    temp->temp_data_arrived = 0;

    temp->psmt_window_size = 0;
    temp->psmt_window_size_min = 0;

    temp->total_packet_count = 0;
    temp->packet_count_td = 0;

    temp->burst_time = 0;
    temp->connection_start_time = 0;
    temp->td_start_time = 0;

    temp->psmt_start_time = 0;
    temp->psmt_time_lapsed = 0;
    temp->connection_time_lapsed = 0;

    temp->total_idle_time = 0;
    temp->sleep_timestamp = 0;

    temp->max_psmt_throughput = INITIAL_MAX_PSMT_THROUGHPUT;
    temp->min_psmt_throughput = INITIAL_MIN_PSMT_THROUGHPUT;

    temp->next = NULL;

    /**
     * Timer initialization
     */
    init_timer(&temp->timer);
    temp->timer.data = (unsigned long) temp;
    temp->timer.function = timer_function;

    init_timer(&temp->sleep_timer);
    temp->sleep_timer.data = (unsigned long) temp;
    temp->sleep_timer.function = sleep_timer_function;

    init_timer(&temp->wake_timer);
    temp->wake_timer.data = (unsigned long) temp;
    temp->wake_timer.function = wake_timer_function;

    // change for testing
    temp->task = NULL;
    // end

    *arg_con = temp;

  } else {
    while (temp->next != NULL)
      temp = temp->next;

    node = kmalloc(sizeof(struct constate), GFP_KERNEL);
    /**
     * Initialize Read Write Spin Lock
     */
    rwlock_init(&node->connection_rwlock);

    node->connection_id = arg_con_id;
    node->src_port = ntohs(arg_tcp->source);
    node->dst_port = ntohs(arg_tcp->dest);
    node->window_size = ntohs(arg_tcp->window);

    node->window_scale = 0;
    node->tcpi_rcv_mss = DEFAULT_MSS_VALUE;

    node->seq_number = ntohs(arg_tcp->seq);
    node->ack_number = ntohs(arg_tcp->ack_seq);

    node->src_addr = ntohl(arg_ip->saddr);
    node->dst_addr = ntohl(arg_ip->daddr);

    node->srtt = 0;
    node->rcv_rtt = 0;

    node->burst_stage = BURST_NO_CONNECTION;
    
    node->rtt = 0;
    node->tcpi_rcv_rtt = 0;
    node->rtt_var = 0;

    node->state = state;
    node->choke_state = NO_OP;
    node->psmt_state = NO_OP;
    node->idle_state = NOT_IDLE; // by default the connection is active

    node->flow_rate_normal = 0;
    node->flow_rate_td = 0;
    node->flow_rate_psmt = 0;
    node->flow_rate_inst = 0;

    node->data_arrived_td = 0;
    node->data_arrived_burst = 0;
    node->temp_data_arrived = 0;

    node->psmt_window_size = 0;
    node->psmt_window_size_min = 0;

    node->total_packet_count = 0;
    node->packet_count_td = 0;

    node->burst_time = 0;
    node->connection_start_time = 0;
    node->td_start_time = 0;
    node->psmt_start_time = 0;
    node->psmt_time_lapsed = 0;
    node->connection_time_lapsed = 0;


    node->chocked = false;
    

    node->total_idle_time = 0;
    node->sleep_timestamp = 0;

    node->max_psmt_throughput = INITIAL_MAX_PSMT_THROUGHPUT;
    node->min_psmt_throughput = INITIAL_MIN_PSMT_THROUGHPUT;

    node->next = NULL;

    /*
     * Timer Initialization
     */
    init_timer(&node->timer);
    node->timer.data = (unsigned long) node;
    node->timer.function = timer_function;

    init_timer(&node->sleep_timer);
    node->sleep_timer.data = (unsigned long) node;
    node->sleep_timer.function = sleep_timer_function;

    init_timer(&node->wake_timer);
    node->wake_timer.data = (unsigned long) node;
    node->wake_timer.function = wake_timer_function;

    // change for testing
    node->task = NULL;
    // end


    temp->next = node;

  }
}

/**
 * Get Connection
 *
 * -Ahmad
 */
static inline struct constate * get_connection(struct constate** arg_con,
                                               u_int16_t connection_id) {

  struct constate *node = *arg_con;

  while (node != NULL) {
    if (node->connection_id == connection_id) {
      //			printk(" 2 - in function   : %d\n", node->connection_id);
      return node;
    }
    node = node->next;
  }

  return NULL;
}

/**
 * Set a connection's state
 *
 * Find the node from the connection id and set the state
 *
 * -Ahmad
 */
static inline u_int8_t set_connection_state(struct constate** arg_con,
                                            u_int16_t arg_port_id, u_int16_t state) {
  struct constate *temp = *arg_con;
  while (temp != NULL) {

    if (temp->connection_id == arg_port_id) {
      temp->state = state;
      return 0;
    }

    temp = temp->next;
  }//end of while

  return -1;
}


static inline void update_rtt(struct constate* node,
                              struct sk_buff* conskb){


  if (node != NULL && conskb != NULL) {
    //refresh_rtt(connection, my_skb);	// If we have to use this function,
    // the time units need to be adjusted
    // from millisecs to nanosecs

    update_tcpi_rtt(node, conskb);
  }

}



/**
 * Update RTT of a connection
 * -Ahmad
 */
static inline u_int8_t refresh_rtt(struct constate* node,
                                   struct sk_buff* conskb) {

  struct tcp_sock* tp = tcp_sk(conskb->sk);

  if (tp) {

    node->srtt = tp->srtt;
    node->rcv_rtt = tp->rcv_rtt_est.rtt;
    return 0;
  }

  return -1;
}


/*
 * Get RTT from tcp_info structure
 */
static inline u_int8_t update_tcpi_rtt(struct constate* node,
                                       struct sk_buff* conskb) {

  struct tcp_info temp_tcp_info;
  struct sock * sk;

  if (node == NULL || conskb == NULL){
    return -1;
  }


  sk = conskb->sk;

  if (sk != NULL ){

    tcp_get_info(sk, &temp_tcp_info);

    if (&temp_tcp_info != NULL){
      node->rtt = temp_tcp_info.tcpi_rtt;
      node->tcpi_rcv_rtt = temp_tcp_info.tcpi_rcv_rtt;
      node->rtt_var = temp_tcp_info.tcpi_rttvar;
    }

  }

  return -1;
}



/*
 * Get Window Scale Value
 */
static inline u_int8_t update_window_scale_value(struct constate* node,
                                                 struct sk_buff* conskb) {

  struct tcp_info temp_tcp_info;
  struct sock * sk;

  if (node == NULL || conskb == NULL){
    return -1;
  }


  sk = conskb->sk;

  if (sk != NULL ){
    /*
     * I know this might be a very very very basic
     * question.. but when I declare a pointer to
     * tcp_info as:
     *
     * struct tcp_info * temp_tcp_info;
     *
     * and give the argument as 'temp_tcp_info'
     * instead of '&temp_tcp_info' as it is in
     * the current case, the following function
     * crashes..
     *
     * isn't it the same thing ?? I know
     * this is stupid...
     *
     */
    tcp_get_info(sk, &temp_tcp_info);

    if (&temp_tcp_info != NULL){
      node->window_scale = temp_tcp_info.tcpi_rcv_wscale;
    }

  }

  return -1;
}

/*
 * Get MSS from tcp_info structure
 */
static inline u_int8_t update_tcpi_rcv_mss(struct constate* node,
                                           struct sk_buff* conskb) {

  struct tcp_info temp_tcp_info;
  struct sock * sk;

  if (node == NULL || conskb == NULL){
    return -1;
  }


  sk = conskb->sk;

  if (sk != NULL ){

    tcp_get_info(sk, &temp_tcp_info);

    if (&temp_tcp_info != NULL){

      /*
       * todo:
       *
       * This function is not working properly.
       * For now the default value of MSS is
       * returned i.e. DEFAULT_MSS_VALUE .. 1452
       */
      node->tcpi_rcv_mss = DEFAULT_MSS_VALUE;
      //			node->tcpi_rcv_mss = temp_tcp_info.tcpi_advmss;

    }

  }

  return -1;
}

/*
 * Get Name of the Current State
 *
 * The state provides information of all the events
 * a connection has gone through. This function provides the
 * name of the current state
 * -Ahmad
 */
static inline char * get_current_state_name(u_int16_t state) {

  int i = 1;
  int temp_state = 1;

  if (state == NO_CONNECTION)
    return state_name[0];

  for (; i < strlen(state_name) - 1; i++, temp_state *= 2) {

    if (state < (temp_state * 2))
      return state_name[i];

  }

  return "Error";
}

/*
 * Get Name of the Choke State
 *
 * -Ahmad
 */
static inline char * get_choke_state_name(u_int8_t choke_state) {

  if (choke_state < strlen(choke_state_name)) return choke_state_name[choke_state];

  return "Error";
}

/*
 * Get Name of the PSMT State
 *
 * -Ahmad
 */
static inline char * get_psmt_state_name(u_int8_t psmt_state) {

  if (psmt_state < strlen(psmt_state_name)) return psmt_state_name[psmt_state];

  return "Error";
}

/* To delete  a specific connection*/
static inline int delete_any(struct constate** arg_con, u_int16_t arg_port_id) {

  struct constate *old, *temp = *arg_con;

  while (temp != NULL) {
    if (temp->connection_id == arg_port_id) {
      /* If it is the beginner node */

      if (temp == *arg_con) {
        *arg_con = temp->next;
        kfree(temp);
        printk(KERN_INFO"%s is called.....Connection %u is deleted....\n", __FUNCTION__,temp->connection_id);
        return 1;
      } else {
        old->next = temp->next;
        kfree(temp);
        printk(KERN_INFO"%s is called.....Connection %u is deleted....\n", __FUNCTION__,temp->connection_id);
        return 1;
      }//end of inner if/else
    } else {
      old = temp;
      temp = temp->next;
    }//end of outer if/else
  }//end of while loop


  /* If the specified connection is not found */
  printk(KERN_INFO"%s: Port %d Connection is not Found....\n", __FUNCTION__,arg_port_id);
  return 0;
}

static inline void delete_all(struct constate** arg_con) {

  struct constate* node, *temp = *arg_con;
  while (temp != NULL) {
    node = temp;
    del_timer(&node->timer);
    del_timer(&node->sleep_timer);
    temp = temp->next;
    kfree(node);
  }

  printk(KERN_INFO"Function '%s' is called, all connections are deleted\n", __FUNCTION__);

}

static inline void dump_constate(struct constate** arg_con) /* Print all the connections */
{
  struct constate *temp = *arg_con;
  while (temp != NULL) {
    printk(KERN_INFO" Connection id %d \n", temp->connection_id);
    temp = temp->next;
  }
}

/**
 * Display all connections
 * Just the basic information - Ahmad
 */

static inline void display_connections_info(struct constate** arg_con) {

  struct constate *node = *arg_con;

  printk (KERN_INFO "Starting to print all connections\n");
  printk (KERN_INFO "====================================================================================================================================================\n");
  printk (KERN_INFO "ID \t State Name     (State#) \t RTT  \t RTT Var +/- \t Data \t FR-N \t FR-TD \t FR-PT \t FR-In \t Choke  PSMT State \t Min T \t Scale \t PSMT \t Idle \t Destination IP\n");
  printk (KERN_INFO "-- \t ----------------------- \t ---- \t ----------- \t ---- \t ---- \t ----- \t ----- \t ----- \t -----  ---------- \t ----- \t ----- \t ---- \t ---- \t --------------\n");

  while (node != NULL) {


    //		if ((node->state & THROTTLE_DETECTION) || (node->state & PSMT)) { // show only active connections
    //		if (1){
    printk (//KERN_INFO
            //"%d \t %s (%d) \t %d \t\t %d \t\t %d \t\t %d \t\t %d \t\t %d \t %d.%d.%d.%d \t %d.%d.%d.%d \n",
            //"%d \t %s (%d) \t %d \t %d-%d-%d \t %d \t\t %d \t %d \t %d \t\t%d \t %d-%s  %d-%s \t %d \t %d \t %d.%d.%d.%d\n",
            "%d \t %s (%d) \t %d \t %d \t\t %d \t %d \t %d \t %d \t %d \t %d-%s  %d-%s \t %d \t %d \t %d \t %d \t %d.%d.%d.%d\n",
            node->connection_id,
            get_current_state_name(node->state),
            node->state,
            node->rtt/1000, 						// from tcp_info
            ((node->rtt_var/1000) > 9999) ? 9999 : node->rtt_var/1000,					// from tcp_info
            node->temp_data_arrived/1024,
            node->flow_rate_normal/1024,
            node->flow_rate_td/1024,
            node->flow_rate_psmt/1024,
            node->flow_rate_inst/1024,
            node->choke_state,get_choke_state_name(node->choke_state),
            node->psmt_state,get_psmt_state_name(node->psmt_state),
            node->min_psmt_throughput, //node->burst_time,
            node->window_scale,
            //				node->connection_time_lapsed/1000,
            node->psmt_time_lapsed/1000,
            node->total_idle_time/1000,
            IPCOMP(node->dst_addr, 0), IPCOMP(node->dst_addr, 1), IPCOMP(node->dst_addr, 2), IPCOMP(node->dst_addr, 3)
            );
    //	}

    node = node->next;
  }
  printk (KERN_INFO "====================================================================================================================================================\n");

  /* if (EMULATE_WNIC) { */
  /* 	printk (KERN_INFO " |       EMULATED WNIC			\n"); */
  /* 	printk (KERN_INFO " | WNIC State is %s				\n", (wnic->idle_state==IDLE)?"IDLE":"ACTIVE"); */
  /* 	printk (KERN_INFO " | Total Idle Time : %d secs		\n", wnic->total_idle_time/1000); */
  /* 	printk (KERN_INFO " | Time Lapsed since first PMST operation : %d secs		\n", wnic->time_lapsed?wnic->time_lapsed/1000:0 ); */
  /* 	printk (KERN_INFO "=================================================================// \n"); */

  /* } */

}

static inline int count(struct constate** arg_con) {
  int con = 0;
  struct constate *temp = *arg_con;
  while (temp != NULL) {

    con++;
    temp = temp->next;
  }

  printk(KERN_INFO" Connection id %d", temp->connection_id);
  return con;
}

static inline void update_flow_rate_normal(struct constate* node){

  struct constate * connection = node;

  u_int32_t time_period = jiffies_to_msecs(jiffies - connection->connection_start_time);

  //	u_int64_t temp_fr = 0;

  if (node == NULL) return;


  /*
   * Calculate the rate of data being transfered
   */
  if (time_period > 0) { // sanity checks
    /*
     * First data packet
     */
    //		temp_fr = (connection->temp_data_arrived * 1000);
    //		do_div(temp_fr,time_period);
    //		connection->flow_rate_normal = temp_fr;

    connection->flow_rate_normal = (connection->temp_data_arrived/ time_period )  * 1000;

    //		printk(" -- total_data: %d, time_period: %d, flow_rate_normal: %d\n",
    //				connection->total_data_arrived, time_period, connection->flow_rate_normal);

  }

}

static inline void update_flow_rate_td(struct constate* node){

  struct constate * connection = node;

  u_int32_t time_period = jiffies_to_msecs(jiffies - connection->td_start_time);
  //	u_int64_t temp_fr = 0;

  if (node == NULL) return;

  /*
   * Calculate the rate of data being transfered
   */
  if (connection->burst_time > 0) { //sanity checks

    //		temp_fr = (connection->data_arrived_td * 1000);
    //		do_div(temp_fr,time_period);
    //		connection->flow_rate_td = temp_fr;

    connection->flow_rate_td = (connection->data_arrived_td	/ time_period)  * 1000;

    //		printk(" -- data_td: %d, burst_time: %d, flow_rate_td: %d\n",
    //						connection->data_arrived_td, time_period, connection->flow_rate_td);

  }
}

static inline void update_flow_rate_psmt(struct constate* node){

  struct constate * connection = node;

  u_int32_t time_period = jiffies_to_msecs(jiffies - connection->psmt_start_time);
  //	u_int64_t temp_fr = 0;

  if (node == NULL) return;


  /*
   * Calculate the rate of data being transfered
   */
  if (time_period > 0) { // sanity checks
    /*
     * First data packet
     */

    //		temp_fr = (connection->temp_data_arrived * 1000);
    //		do_div(temp_fr,time_period);
    //		connection->flow_rate_psmt = temp_fr;

    connection->flow_rate_psmt = (connection->temp_data_arrived	/ time_period )  * 1000;

    //		printk(" -- data: %d, time_period: %d, flow_rate_psmt: %d\n",
    //				connection->temp_data_arrived, time_period, connection->flow_rate_psmt);

  }

}

static inline u_int32_t calculate_window_size(u_int32_t flowrate_normal , u_int32_t rtt){

  return (flowrate_normal*rtt)/1000;

}

static inline void reduce_window_size(struct constate * connection){

  if (connection == NULL) return;
  if ( (signed int)( connection->psmt_window_size - connection->tcpi_rcv_mss)	>= ( signed int)connection->psmt_window_size_min) { // casting to int to get signed values

    connection->psmt_window_size -= connection->tcpi_rcv_mss;
    printk(	" -- Dec WinSize <psmt_window_size : %u , psmt_window_size_min : %u> \n",connection->psmt_window_size, connection->psmt_window_size_min);
  }

}
/**
 * Check the state of all the Connection i.e.
 * if all of them are idle or not.. also see if
 * any connection is about to wake up, in that
 * case, the wnic should not sleep.
 *
 * Returns a boolean value i.e. 1 is returned
 * if all the connections are idle and are predicted
 * to remain idle for more than a certain amount of time.
 * -Ahmad
 */
static inline int sleep_wnic_check(struct constate** arg_con, u_int32_t arg_con_id ) {

  struct constate *node = *arg_con;
  //unsigned long time_to_wake = 0;

  while (node != NULL) {

    if (node->idle_state == NOT_IDLE) {

      /* if (EMULATE_WNIC) { */
      /* 	/\* */
      /* 	 * todo: */
      /* 	 * Only checking the connections that are in PSMT state */
      /* 	 * for testing purposes.. */
      /* 	 * */
      /* 	 * In real life scenario, all connections must be checked */
      /* 	 *\/ */
      /* 	if ((node->state == (SYNED | ACKED | ESTABLISHED | THROTTLE_DETECTION) */
      /* 		|| node->state == (SYNED | ACKED | ESTABLISHED | THROTTLE_DETECTION | PSMT)) */
      /* 			&& (node->connection_id != arg_con_id)) { */

      /* 		printk(KERN_INFO " -- some other connection active\n"); */
      /* 		return 0; */

      /* 	} */

      /* } else { */
      /* 	printk(KERN_INFO " -- some other connection active\n"); */
      /* 	return 0; */
      /* } */

    }

    //		else if (node->idle_state == IDLE){
    //			/*
    //			 * Check if all connections will remain idle
    //			 * for more than a specific amount of time
    //			 * i.e Transition time from sleep to wake state
    //			 */
    //			time_to_wake = node->wake_timestamp - jiffies;
    //			if (jiffies_to_msecs(time_to_wake) <= TRANSITION_TIME_SLEEP_TO_WAKE) {
    //				 /*
    //				  * if jiffies is more than the wake_timestamp, it can cause probs
    //				  * but no.. because the vars are unsigned and the result will be huge !
    //				  * instead of -ve.. hence satisfying the condition.
    //				  */
    //				printk(KERN_INFO "--- time to wake : %d (%d msecs), wake_timestamp: %d \n", time_to_wake, jiffies_to_msecs(time_to_wake), node->wake_timestamp);
    //				return 0;
    //			}
    //		}

    node = node->next;
  }

  return 1;
}


/*
 * Scheduler is called every time the idle_state
 * of a connection changes.. On calling the scheduler,
 * it checks all the connection states and sets the
 * device state to sleep or awake accordingly.
 *
 * The parameter account_for_transitions is a boolean value
 * If true, the transition times are subtracted from the
 * total idle time (only if EMULATE_WNIC is 1)
 */
static inline void scheduler(struct constate * node, int idle_state, char * log_message){

  struct constate * connection = node;
  u_int32_t connection_idle_period = 0;
  u_int32_t wnic_idle_period = 0;
  //unsigned long now_timestamp = jiffies;

  if (node == NULL) return; // sanity

  node->connection_time_lapsed = jiffies_to_msecs(jiffies - connection->connection_start_time);
  node->psmt_time_lapsed = jiffies_to_msecs(jiffies - connection->psmt_start_time);

  /* if (EMULATE_WNIC && wnic->initial_timestamp){ */
  /* 	wnic->time_lapsed = jiffies_to_msecs(jiffies-wnic->initial_timestamp); */
  /* } */

  switch(idle_state){

  case IDLE:
    /*
     * change the idle_state of the connection
     * it has nothing to do with wnic state
     */
    if (node->idle_state == NOT_IDLE) {
      node->idle_state = IDLE;
      //			if (account_for_transitions) {
      //				node->total_idle_time -= TRANSITION_TIME_WAKE_TO_SLEEP;
      //			}
      node->sleep_timestamp = jiffies; // start sleeping
    }

    /*
     * Change the state of the WNIC
     */
    //		if (EMULATE_WNIC) {

    //	if (wnic->idle_state == NOT_IDLE) /* { */

    /* 				/\* */
    /* 				 * If all connections are idle and are predicted */
    /* 				 * to remain in the same state, transition wnic */
    /* 				 * to sleep state */
    /* 				 *\/ */
    /* 				if (sleep_wnic_check(&all_connections_head, node->connection_id)) { */
    /* 					wnic->idle_state = IDLE; */

    /* //					if (account_for_transitions) { */
    /* //						wnic->total_idle_time -= TRANSITION_TIME_WAKE_TO_SLEEP; */
    /* //					} */
    /* 					wnic->sleep_timestamp = jiffies; */
    /* 					printk("SLEEP : %s\n", log_message); */
    /* 				} else { */
    /* 					printk("< no time to sleep >\n"); */
    /* 				} */

    /* 			} */

    //		} else {

    /* if (is_wnic_sleep()) { */
    /* 	if (sleep_wnic_check(&all_connections_head, node->connection_id)) { */
    /* 		wni_control(IDLE); */
    /* 		printk("SLEEP : %s\n", log_message); */
    /* 	} else { */
    /* 		printk("< no time to sleep >\n"); */
    /* 	} */
    /* } */

    //		}

    break;

  case NOT_IDLE:
    /*
     * Connection idle_state
     */
    if (node->idle_state == IDLE) {
      node->idle_state = NOT_IDLE;
      connection_idle_period = jiffies_to_msecs(jiffies - node->sleep_timestamp);
      node->total_idle_time += connection_idle_period;

      //			if (account_for_transitions) {
      //				node->total_idle_time -= TRANSITION_TIME_SLEEP_TO_WAKE;
      //			}

    }


    /*
     * Modify the state of the WNIC based on the signal
     */

    /* 		if (EMULATE_WNIC) { */


    /* 			if (wnic->idle_state == IDLE) { */

    /* 				wnic_idle_period = jiffies_to_msecs(jiffies - wnic->sleep_timestamp); */

    /* 				wnic->idle_state = NOT_IDLE; */
    /* 				wnic->total_idle_time += wnic_idle_period; */
    /* //				if (account_for_transitions){ */
    /* //					wnic->total_idle_time -= TRANSITION_TIME_SLEEP_TO_WAKE; */
    /* //				} */



    /* 				printk("AWAKE : %s ( %d msecs) \n", log_message, wnic_idle_period); */

    /* 				/\** */
    /* 				 * Printing .. for Results */
    /* 				 *\/ */
    /* 				printk("%ludconnection%d|%d|%d|%lud \n", test_identifier, */
    /* 										node->connection_id, */
    /* 										node->connection_time_lapsed, */
    /* 										node->total_idle_time, */
    /* 										node->flow_rate_psmt); */

    /* 				printk("%ludwnic|%d|%ud| \n", test_identifier, */
    /* 						wnic->time_lapsed, */
    /* 						wnic->total_idle_time); */


    /* 			} */
    /* 		} else { */

    /* 			wni_control(NOT_IDLE); */
    /* 		} */

    break;

    //	/**
    //	 * The EMULATE cases are only called if the WNIC is being emulated
    //	 */
    //	case EMULATE_SINGLE_PACKET:
    //		if (node->idle_state == IDLE){
    //			node->total_idle_time -= SEND_SINGLE_PACKET_TIME;
    //		}
    //
    //		if (wnic->idle_state == IDLE){
    //			wnic->total_idle_time -= SEND_SINGLE_PACKET_TIME;
    //		}
    //
    //		break;

  default:
    printk("ERROR : %s\n", log_message);
    break;
  }



}

/**
 * Sleep Timer Function
 *
 * Sleep timer: Used to switch from awake to sleep modes
 * of the underlying wireless device
 * Controls the state of the wifi device
 *
 * By default, the sleep timer is being used in the local out
 * hook right after the window is advertised in the
 * ADVERTISE_WINSIZE state in PSMT.
 *
 * -Ahmad
 */
static inline void sleep_timer_function(unsigned long data) {

  struct constate *connection = (struct constate *) data;
  if (connection == NULL) return;

  /*
   * Set the wifi device to awake mode
   * (if in Sleep Mode at the moment)
   */
  scheduler(connection, NOT_IDLE, "sleep timer up.. waking up");

}

/*
 * The sleep timer is used when the connection goes to idle mode
 * and is required to get up after a certain amount of 'wait_time'
 *
 * The function modifies the sleep_timer and also updates the
 * wake_timestamp which is later on used by the scheduler to see if
 * any connection is about to wake up or not.
 */
static inline void modify_sleep_timer(struct constate *connection, const unsigned int wait_time){

  unsigned long expires = jiffies + msecs_to_jiffies(wait_time);

  mod_timer(&connection->sleep_timer, expires);
  connection->wake_timestamp = expires;
}


/**
 * Wake Timer Function
 *
 * Wake timer: Used to switch to awake mode when the deviced
 * is (supposedly) sleeping.
 *
 * The wake timer is used when the device is sleeping after
 * having received a packet burst and needs to wake up just to
 * send an acknowledgement.
 *
 * -Ahmad
 */
static inline void wake_timer_function(unsigned long data) {

  struct constate *connection = (struct constate *) data;
  if (connection == NULL) return;

  /*
   * Set the wifi device to sleep mode after having staying
   * up for specified amount of time
   */
  scheduler(connection, IDLE, "Advertised.. going back to sleep");

}


/**
 * Timer Function
 *
 * Performs the following functions according to
 * the state of the connection:
 *
 *  -	Change a connection's state to
 *  	Throttle detection
 *  - 	Choke/Unchoke during throttle detection
 *
 * -Ahmad
 */
static inline void timer_function(unsigned long data) {

  struct constate *node = (struct constate *) data;
  if (node != NULL) {

    if (node->state == (SYNED | ACKED | ESTABLISHED)) {

      if (node->choke_state == NO_OP) {

        node->choke_state = CALC_FLOWRATE;

        mod_timer(&node->timer, jiffies
                  + msecs_to_jiffies(calc_flowrate_wait));

        node->flow_rate_normal = 0;
        node->temp_data_arrived = 0;
        node->connection_start_time = jiffies;

      } else if (node->choke_state == CALC_FLOWRATE) {

        if (node->total_packet_count >= PACKET_COUNT_THRESHOLD
            && node->temp_data_arrived >= DATA_THRESHOLD) {

          node->state |= THROTTLE_DETECTION;
          node->choke_state = PRE_CHOKE;
          node->td_start_time = jiffies;
        }

      }

    } else if (node->state == (SYNED | ACKED | ESTABLISHED | THROTTLE_DETECTION)) {
      /*
       * Change the state to NO_OP, which means
       * that the LOCAL_IN hook will stop calculating
       * the flow rate for the bursty traffic after
       * choking the connection once.
       */
      node->choke_state = NO_OP;

      if (node->flow_rate_normal == 0 || node->flow_rate_td == 0){

        node->state |= NORMAL;

      } else if ( ENABLE_PSM_THROTTLING(node->flow_rate_normal, node->flow_rate_td,BANDWIDTH_RATIO ) ){

        node->state |= PSMT;
        node->psmt_state = INITIAL_CHOKE;

        /*
         * Preparing for PSM State..
         * the following statements can also be given after
         * INITIAL_WAIT to have a more accurate calculation of
         * flow rate during PSMT
         */
        node->temp_data_arrived = 0; // reset the data arrived for calculation of flow rate
        node->psmt_start_time = jiffies; // mark the time when we enter psmt state

        //				if (EMULATE_WNIC){
        //					if (wnic->initial_timestamp == 0){ // only update the first time
        //						wnic->initial_timestamp = node->psmt_start_time;
        //					}
        //				}
        node->psmt_window_size_min = calculate_window_size(node->flow_rate_normal, node->rtt/1000);
        node->psmt_window_size = node->psmt_window_size_min;


      } else {
        node->state |= NORMAL;
      }

    }  else if (node->state == (SYNED | ACKED | ESTABLISHED | THROTTLE_DETECTION | PSMT)) {

      switch (node->psmt_state){

      case INITIAL_CHOKE:
      case INITIAL_WAIT:
        /*
         * Preparing for PSM State..
         */
        node->temp_data_arrived = 0; // reset the data arrived for calculation of flow rate
        node->psmt_start_time = jiffies; // mark the time when we enter psmt state

        /* if (EMULATE_WNIC){ */
        /* 	if (wnic->initial_timestamp == 0){ // only update the first time */
        /* 		wnic->initial_timestamp = node->psmt_start_time; */
        /* 	} */
        /* } */
        node->psmt_state = ADVERTISE_WINDOW_SIZE;
        printk (" ( |/ ) INITIAL_WAIT -> %s\n", get_psmt_state_name(node->psmt_state) );
        break;


      case ADVERTISE_WINDOW_SIZE:
        /*
         * Do nothing.. connection is already in
         * ADVERTISE_WINDOW_SIZE state.
         */
        //				printk(" ( |/ ) ADVERTISE_WINDOW_SIZE -> %s\n",	get_psmt_state_name(node->psmt_state));
        break;

      case CHOKE:
      case RECEIVE_PACKETS:

        /*
         * We enter this state because:
         * 1. either the advertised window size is too big
         * 2. The expected throughput is too big (which requires a big window adv)
         * 3. some problem in the network caused the delay in packet arrival
         *
         * Need to readvertise a new window size.. and adjust the parameters accordingly
         */

        node->data_arrived_burst = 0;
        node->psmt_state= ADVERTISE_WINDOW_SIZE;

        /*
         * Decrease the psmt min throughput
         */
        //				if (node->min_psmt_throughput > 50){
        //					node->min_psmt_throughput -= 1;
        //				}
        //				reduce_window_size(node);
        printk (" ( |/ ) RECEIVE_PACKETS -> entering %s state\n", get_psmt_state_name(node->psmt_state) );


        break;



      default:
        break;
      }

    }
  }
}
