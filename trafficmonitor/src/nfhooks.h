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

#ifndef NFHOOKS_H_
#define NFHOOKS_H_

/**
 * Netfilter includes
 */
#include <linux/ip.h>
#include <linux/tcp.h>
#include <linux/netfilter.h>
#include <linux/netfilter_ipv4.h>
#include <net/ip.h>
#include <net/tcp.h>
#include <linux/time.h>
//#include "connections.h"
//#include "wireless.h" 					// is_wnic_sleep();
#include "common.h"

#define BURST_SIZE_RATIO 3
#define NETWORK_DELAY_FACTOR 4
#define BURST_THRESHOLD_BEGINNING  10  // The threshold indicating the beginning of the actual burst traffic

//static struct nf_hook_ops post_nfho, pre_nfho;

static inline void set_tcp_window_size(struct sk_buff* my_skb,struct tcphdr* tcph, struct iphdr* iph, u_int32_t window_size, u_int16_t window_scale);

extern void newPacket(u_int32_t connection_id, unsigned int size, int direction, bool newConnection);

static bool OnChocking = false;

inline long long unsigned gettime() {
        struct timeval tv;
        do_gettimeofday(&tv);
        return ((long long unsigned)tv.tv_sec * 1000000UL + (long long unsigned)tv.tv_usec);
}
/**
 * LOGGING HELPER
 */
#define DIR_IN 0
#define DIR_OUT 1

static void print_packet_info(u_int32_t connection_id, struct tcphdr* tcph, struct iphdr* iph, struct sk_buff* skb, const int direction, const char* packet_type) 
{
    printk(KERN_INFO "TM %s %u %s, ack %u, seq %u, size %u",
            direction == DIR_IN ? "<=" : "=>",
            connection_id,
            packet_type,
            tcph->ack_seq,
            tcph->seq,
            skb->len);
}

/**
 * NETFILTER HOOKS
 */

static unsigned int hook_local_in(unsigned int hooknum, struct sk_buff *skb, const struct net_device *in, const struct net_device *out, int(*okfn)(struct sk_buff *))
{
  struct tcphdr* tcph;
  struct iphdr* iph;
  struct sk_buff* my_skb;
  
  u_int32_t connection_id;
 
  unsigned int skb_len = 0;
  unsigned int iph_len = 0;
  unsigned int tcph_len = 20;		// TCP header
  unsigned int tcp_opt_len = 0;	// TCP options
  unsigned int tcp_payload = 0;
  unsigned int segment_size = 0;	// TCP Options + TCP payload,
									// corresponds to the MSS value
									// set during TCP Handshake

  my_skb = skb;

  if (!my_skb)
  {
    printk(KERN_INFO "Hook: skb error\n");
    return NF_ACCEPT;
  }
  
  skb_len = my_skb->len;


  iph = ip_hdr(my_skb);
  if (!iph)
  {
    printk(KERN_INFO "Hook: IP header error\n");
    return NF_ACCEPT;
  }


  if (iph->protocol != 6)
  {
#ifdef OTHER_PACKET
    printk(KERN_INFO "TM <= Not a TCP packet. Protocol: %u\n", iph->protocol);
#endif
    return NF_ACCEPT; // if not TCP packet

  }
  

  iph_len = ip_hdrlen(my_skb);

  /**
   * Even though the following statement should
   * return the tcp header but it only works in case
   * of outgoing packets.
   *
   * tcph = tcp_hdr(my_skb);
   *
   * Therefore we come up with our own way of casting.
   *
   * This problem only occurs from the incoming tcp
   * packets.. maybe the headers are not initialized
   * or what?
   */

  //temporary modification for testing
  tcph = (struct tcphdr *) (my_skb->data + iph->ihl * 4);
  //struct tcphdr *tcph2 = (struct tcphdr *) skb_transport_header(my_skb);

  /*
   * We have noticed that for wired ethernet connections,
   * getting the tcph in the above manner works, but in
   * case of wireless networks, tcph incorrectly points
   * to the tcp flags like syn, ack, doff etc.
   *
   * todo: In order to cater for that, we need to get these
   * values manually.
   */

  tcp_opt_len = (tcph->doff * 4) - (sizeof(struct tcphdr));


  /**
   * Calculating TCP Payload
   */
  tcp_payload = skb_len - tcph_len - tcp_opt_len - iph_len;
  //  tcp_payload = skb_len - iph_len;
  
  segment_size = tcp_payload + tcp_opt_len;
  
  

  /*
   * Connection id is destination port.. for incoming traffic
   */
  connection_id = ntohs(tcph->dest);

  struct constate *connection = get_connection(&all_connections_head, connection_id);

  if ((connection == NULL)) {
    
#ifdef ONLY_SHOW_SYN_ACK
    //printk(KERN_INFO "TM <= New connection: the connection ID: %u, SYN bit: %u, ACK bit: %u", connection_id, tcph->syn, tcph->ack);
    //printk(KERN_INFO "TM <= %u new, ack %u", connection_id, tcph->ack);
    print_packet_info(connection_id, tcph, iph, skb, DIR_IN, "new");
    return NF_ACCEPT;
#endif
    
    if (tcph->syn) {
      
      if(tcph->ack)
      {
        // The last state parameter is not used any more. So don't bother if it is SYNED or CLOSED
        add_connection(&all_connections_head, iph, tcph, connection_id, SYNED);
        struct constate *newConnection = get_connection(&all_connections_head, connection_id);
        newConnection->burst_stage = SYN_ACK;

        // We don't deal with such traffic because we assume the module is loaded before any TCP traffic starts

#ifdef DISPLAY_BURST_STAGE
        //printk(KERN_INFO "TM <= Got a SYN-ACK packet for a non-existing connection. With connection ID: %u and ACK value: %u. Might be the connection had been established before EC module started. The connection was added to the list and the stage tis SYN-ACK\n", connection_id, tcph->ack_seq);
        //printk(KERN_INFO "TM <= %u new_synack, ack %u\n", connection_id, tcph->ack_seq);
        print_packet_info(connection_id, tcph, iph, skb, DIR_IN, "new_synack");
#endif
      }
      
      else
      {
        // The last state parameter is not used any more. So don't bother if it is SYNED or CLOSED
        add_connection(&all_connections_head, iph, tcph, connection_id, SYNED);
        struct constate *newConnection = get_connection(&all_connections_head, connection_id);
        newConnection->direction = 1;
        newConnection->burst_stage = SYN;

#ifdef DISPLAY_BURST_STAGE
        //printk(KERN_INFO "TM <= Got a SYN packet for a non-existing connection. With connection ID: %u and ACK value: %u. Should not happen currently since mobile is not acting as server.  \n", connection_id, tcph->ack_seq);
        //printk(KERN_INFO "TM <= %u new_syn, ack %u\n", connection_id, tcph->ack_seq);
        print_packet_info(connection_id, tcph, iph, skb, DIR_IN, "new_syn");
#endif
        
      }
      

    } else if (tcph->fin) {

      // The last state parameter is not used any more. So don't bother if it is SYNED or CLOSED
      add_connection(&all_connections_head, iph, tcph, connection_id, SYNED);
      struct constate *newConnection = get_connection(&all_connections_head, connection_id);
      newConnection->burst_stage = FIN;

      // We don't deal with such traffic because we assume the module is loaded before any TCP traffic starts

#ifdef DISPLAY_BURST_STAGE
      //printk(KERN_INFO "TM => Got a FIN packet for a non-existing connection. With conneciton ID: %u and ACK value: %u. Might be the connection had been established before EC module started. The connection was added to the list and the stage tis FIN", connection_id, tcph->ack_seq);
      //printk(KERN_INFO "TM <= %u new_fin, ack %u\n", connection_id, tcph->ack_seq);
      print_packet_info(connection_id, tcph, iph, skb, DIR_IN, "new_fin");
#endif   

    } else if(tcp_payload > BURST_THRESHOLD_BEGINNING)
    {
      // The last state parameter is not used any more. So don't bother if it is SYNED or CLOSED
      add_connection(&all_connections_head, iph, tcph, connection_id, SYNED);
      struct constate *newConnection = get_connection(&all_connections_head, connection_id);
      newConnection->burst_stage = BURST_START;

      // We don't deal with such traffic because we assume the module is loaded before any TCP traffic starts

#ifdef DISPLAY_BURST_STAGE
      //printk(KERN_INFO "TM => Got a normal burst packet for a non-existing connection. With conneciton ID: %u and ACK value: %u. Might be the connection had been established before EC module started. The connection was added to the list and the stage tis BURST_START\n", connection_id, tcph->ack_seq);
      //printk(KERN_INFO "TM <= %u new_burst, ack %u\n", connection_id, tcph->ack_seq);
      print_packet_info(connection_id, tcph, iph, skb, DIR_IN, "new_burst");
#endif      
    }

    // We don't deal with such traffic because we assume the module is loaded before any TCP traffic starts
    else {
#ifdef DISPLAY_BURST_STAGE
//      printk(KERN_INFO "TM <= Got an unknown type packet for a non-existing connection. With conneciton ID: %u and ACK value: %u. Since there is no way to know the status of the connection, it is not added to the connection list.\n", connection_id, tcph->ack_seq);
#endif
    }
    
  } 

  else { // connection != NULL

    
    refresh_rtt(connection, my_skb);
    update_tcpi_rtt(connection, my_skb);


#ifdef ONLY_SHOW_SYN_ACK
    printk(KERN_INFO "TM <= Existing connection: the connection ID: %u, SYN bit: %u, ACK bit: %u",connection_id, tcph->syn, tcph->ack);
    return NF_ACCEPT;
#endif

    
    if (connection->burst_stage == SYN && tcph->ack)
    {
      connection->burst_stage = SYN_ACK;
#ifdef DISPLAY_BURST_STAGE
      //printk(KERN_INFO "TM <= burst_stage changed to SYN_ACK. With connection ID: %u and ACK value: %u \n", connection_id, tcph->ack_seq);
      //printk(KERN_INFO "TM <= %u burst_synack, ack %u\n", connection_id, tcph->ack_seq);
      print_packet_info(connection_id, tcph, iph, skb, DIR_IN, "burst_synack");
#endif
      //      connection->direction = 0;

      // TODO: Generate NEW_BURST event.
      
    }
    
    else if (connection->burst_stage==SYN_ACK && tcph->ack)
    {
      connection->burst_stage = BURST_ESTABLISHED;
#ifdef DISPLAY_BURST_STAGE
      //printk(KERN_INFO "TM <= burst_stage changed to BURST_ESTABLISHED. With connection ID: %u and ACK value: %u. Should not happen currently since mobile is not acting as server \n", connection_id, tcph->ack_seq);
      //printk(KERN_INFO "TM <= %u burst_established, ack %u\n", connection_id, tcph->ack_seq);
      print_packet_info(connection_id, tcph, iph, skb, DIR_IN, "burst_established");
#endif
      
    }
    else if(connection->burst_stage == BURST_ESTABLISHED)
    {
      connection->burst_stage == BURST_REQUEST;

#ifdef DISPLAY_BURST_STAGE
      //printk(KERN_INFO "TM <= Got the burst request packet burst packet. With connection ID: %u and ACK value: %u.  Burst_stage changed to BURST_REQUEST.Should not happen currently since the mobile is not acting as server. \n", connection_id, tcph->ack_seq);
      //printk(KERN_INFO "TM <= %u burst_request, ack %u\n", connection_id, tcph->ack_seq);
      print_packet_info(connection_id, tcph, iph, skb, DIR_IN, "burst_request");
#endif

      struct timeval tv;
      do_gettimeofday(&tv);
      unsigned long timeCurrent = tv.tv_sec*1000000+tv.tv_usec;
      connection->timeStamp = timeCurrent;
 
      // TODO:
    }


    else if(connection->burst_stage == BURST_REQUEST)
    {
      connection->burst_stage = BURST_START;

#ifdef DISPLAY_BURST_STAGE
      //printk(KERN_INFO "TM <= Got the first real burst packet. With connection ID: %u and ACK value: %u.  Burst_stage changed to BURST_REQUEST. \n", connection_id, tcph->ack_seq);
      //printk(KERN_INFO "TM <= %u burst_request, ack %u\n", connection_id, tcph->ack_seq);
      print_packet_info(connection_id, tcph, iph, skb, DIR_IN, "burst_request");
#endif
      
      newPacket(connection_id, tcp_payload, 1, true);
    }
    
    else if(connection->burst_stage == BURST_START)
    {

      if(connection->direction==0) // when this is a download flow. We only send the real tcp traffic on the main direction
      {

#ifdef DISPLAY_BURST_STAGE
        //printk(KERN_INFO "TM <= Got another burst packet. With connection ID: %u and ACK value: %u.\n", connection_id, tcph->ack_seq);
        //printk(KERN_INFO "TM <= %u burst, ack %u\n", connection_id, tcph->ack_seq);
        print_packet_info(connection_id, tcph, iph, skb, DIR_IN, "burst");
#endif
        
        newPacket(connection_id, tcp_payload, 1, false);
      }
      else
      {

#ifdef DISPLAY_BURST_STAGE
        //printk(KERN_INFO "TM <= This is just an ACK packet to the real burst packet on the upload direction. With the connection ID: %u.  Hence it is not part of the burst traffic.\n", connection_id);
        //printk(KERN_INFO "TM <= %u ack_only, ack %u\n", connection_id, tcph->seq);
        print_packet_info(connection_id, tcph, iph, skb, DIR_IN, "ack_only");
#endif
        
      }
      
    }
  }

#ifndef ONLY_SHOW_SYN_ACK
  return NF_ACCEPT;
#endif
}


static unsigned int hook_local_out(unsigned int hooknum, struct sk_buff *skb, const struct net_device *in, const struct net_device *out, int(*okfn)(struct sk_buff *))
{
  struct tcphdr* tcph;
  struct iphdr* iph;
  struct sk_buff* my_skb;
  u_int32_t connection_id;
  my_skb = skb;
  if (!my_skb)
  {
    printk(KERN_INFO "TM => skb NULL\n");
    return NF_ACCEPT;
  }
  
  iph = ip_hdr(my_skb);
  if (!iph)
  {
    printk(KERN_INFO "TM => IP header error\n");
    return NF_ACCEPT;
  }
  
  if (iph->protocol != 6) // if not TCP packet
  {
    printk(KERN_INFO "TM => Not a TCP packet. Protocol: %u ", iph->protocol);
    return NF_ACCEPT;
  }

  unsigned int iph_len = ip_hdrlen(my_skb);
  tcph = tcp_hdr(my_skb);

  // To calculate the packet size
  unsigned int skb_len = my_skb->len;
  
  //unsigned int tcp_opt_len = tcp_optlen(my_skb);
  //unsigned int tcph_len = tcp_hdrlen(my_skb);
  //unsigned int iph_len = iph->tot_len;

  // Casting method
  unsigned int tcph_len = 20;
  struct tcphdr* tcph2 = (struct tcphdr *) (my_skb->data + iph->ihl * 4);
  unsigned tcp_opt_len = (tcph2->doff * 4) - (sizeof(struct tcphdr));
  
  unsigned int tcp_payload = skb_len - tcph_len - tcp_opt_len - iph_len;

  
  /*
   * Connection id is source port.. for outgoing traffic
   */
  
  connection_id = ntohs(tcph->source);
#ifdef PRINT_PACKET
  printk(KERN_INFO "Hook: connection ID: %u", connection_id );
#endif


    
  /*
   * Get Connection from connection_id
   */
  struct constate *connection = get_connection(&all_connections_head, connection_id);

  if ((connection == NULL)) {

#ifdef ONLY_SHOW_SYN_ACK
    // printk(KERN_INFO "TM => New connection: the Connection ID: %u, SYN bit: %u, ACK bit: %u", connection_id, tcph->syn, tcph->ack);
    //printk(KERN_INFO "TM => %u new, ack %u", connection_id, tcph->ack);
      print_packet_info(connection_id, tcph, iph, skb, DIR_OUT, "new");
      return NF_ACCEPT;
#endif
    
    if (tcph->syn) {
      if(tcph->ack)
      {
        add_connection(&all_connections_head, iph, tcph, connection_id, ACKED);
        struct constate *newConnection = get_connection(&all_connections_head, connection_id);
        newConnection->burst_stage = SYN_ACK;

        // We don't deal with such traffic because we assume the module is loaded before any TCP traffic starts

#ifdef DISPLAY_BURST_STAGE
        //printk(KERN_INFO "TM => Sent a SYN-ACK packet for a non-existing connection. With conneciton ID: %u and ACK value: %u. Might be the connection had been established before EC module started. The connection was added to the list and the stage tis SYN-ACK.\n", connection_id, tcph->ack_seq);
        //printk(KERN_INFO "TM => %u new_synack, ack %u\n", connection_id, tcph->ack_seq);
        print_packet_info(connection_id, tcph, iph, skb, DIR_OUT, "new_synack");
#endif
        
      }
      
      else
      {
        add_connection(&all_connections_head, iph, tcph, connection_id, SYNED);
        struct constate *newConnection = get_connection(&all_connections_head, connection_id);
        newConnection->burst_stage = SYN;
        newConnection->direction = 0;

#ifdef DISPLAY_BURST_STAGE
        //printk(KERN_INFO "TM => Sent a SYN packet for a non-existing connection. Burst_stage changed to SYN. With connection ID: %u and ACK value: %u \n", connection_id, tcph->ack_seq);
        //printk(KERN_INFO "TM => %u new_syn, ack %u\n", connection_id, tcph->ack_seq);
        print_packet_info(connection_id, tcph, iph, skb, DIR_OUT, "new_syn");
#endif
      }

    }  // if tcph->syn
    
    else if (tcph->fin) { // for debugging.
      add_connection(&all_connections_head, iph, tcph, connection_id, CLOSED);
      struct constate *newConnection = get_connection(&all_connections_head, connection_id);
      newConnection->burst_stage = FIN;

      // We don't deal with such traffic because we assume the module is loaded before any TCP traffic starts
      

#ifdef DISPLAY_BURST_STAGE
    //printk(KERN_INFO "TM => Sent a FIN packet for a non-existing connection. With conneciton ID: %u and ACK value: %u. Might be the connection had been established before EC module started. The connection was added to the list and the stage tis FIN.\n", connection_id, tcph->ack_seq);
    //printk(KERN_INFO "TM => %u new_fin, ack %u \n", connection_id, tcph->ack_seq);
        print_packet_info(connection_id, tcph, iph, skb, DIR_OUT, "new_fin");
#endif
      
    } // if tcph->fin

    else if(tcp_payload > BURST_THRESHOLD_BEGINNING)
    {
      add_connection(&all_connections_head, iph, tcph, connection_id, ACKED);
      struct constate *newConnection = get_connection(&all_connections_head, connection_id);
      newConnection->burst_stage = BURST_START;

   // We don't deal with such traffic because we assume the module is loaded before any TCP traffic starts
      
#ifdef DISPLAY_BURST_STAGE
      //printk(KERN_INFO "TM => Sent a burst traffic packet for a non-existing connection. With conneciton ID: %u and ACK value: %u. Might be the connection had been established before EC module started. The connection was added to the list and the stage is BURST_START.\n", connection_id, tcph->ack_seq);
      //printk(KERN_INFO "TM => %u new_burst, ack %u\n", connection_id, tcph->ack_seq);
      print_packet_info(connection_id, tcph, iph, skb, DIR_OUT, "new_burst");
#endif
    } 
    
    else {
      // We don't deal with such traffic because we assume the module is loaded before any TCP traffic starts


#ifdef DISPLAY_BURST_STAGE
      //printk(KERN_INFO "TM => Sent an unknown type packet for a non-existing connection. With conneciton ID: %u and ACK value: %u. Since there is no way to know the status of the connection, it is not added to the connection list.\n", connection_id, tcph->ack_seq);
      //printk(KERN_INFO "TM => %u new_unknown, ack %u\n", connection_id, tcph->ack_seq);
      print_packet_info(connection_id, tcph, iph, skb, DIR_OUT, "new_unknown");
#endif
      
      return NF_ACCEPT;
    }

    connection = get_connection(&all_connections_head, connection_id);
  }
  else {  // connection != NULL : connection already exists

    
    update_rtt(connection, my_skb);


#ifdef ONLY_SHOW_SYN_ACK
    printk(KERN_INFO "TM => Existing connection: the connection ID: %u, SYN bit: %u, ACK bit: %u", connection_id, tcph->syn, tcph->ack);
    return NF_ACCEPT;
#endif


    
    if (connection->burst_stage==SYN_ACK && tcph->ack)
    {

      connection->burst_stage = BURST_ESTABLISHED;

#ifdef DISPLAY_BURST_STAGE
//printk(KERN_INFO "TM => burst_stage changed to BURST_ESTABLISHED and send one ACK packet back to user remote server. With connection ID: %u and ACK value: %u. Waiting for real burst.\n ", connection_id, tcph->ack_seq);
      //printk(KERN_INFO "TM => %u burst_established, ack %u\n ", connection_id, tcph->ack_seq);
      print_packet_info(connection_id, tcph, iph, skb, DIR_OUT, "burst_established");
#endif
      
    }
    
    //    else if(connection->burst_stage == BURST_ESTABLISHED && tcp_payload > BURST_THRESHOLD_BEGINNING)
    else if(connection->burst_stage == BURST_ESTABLISHED)
    {
      //      connection->burst_stage = BURST_START;
      connection->burst_stage = BURST_REQUEST;

#ifdef DISPLAY_BURST_STAGE
      //printk(KERN_INFO "TM => Sent the burst request packet. With connection ID: %u and ACK value: %u.  Burst_stage changed to BURST_REQUEST. \n", connection_id, tcph->ack_seq);
      //printk(KERN_INFO "TM => %u burst_request, ack %u\n", connection_id, tcph->ack_seq);
      print_packet_info(connection_id, tcph, iph, skb, DIR_OUT, "burst_request");
#endif
      
      struct timeval tv;
      do_gettimeofday(&tv);
      unsigned long timeCurrent = tv.tv_sec*1000000+tv.tv_usec;
      connection->timeStamp = timeCurrent;

    }


    else if(connection->burst_stage == BURST_REQUEST)
    {
      connection->burst_stage = BURST_START;

#ifdef DISPLAY_BURST_STAGE
      //printk(KERN_INFO "TM => Sent the first real burst packet. With connection ID: %u and ACK value: %u.  Burst_stage changed to BURST_REQUEST. Should not happen currently since the mobiel is not acting as server \n", connection_id, tcph->ack_seq);
      //printk(KERN_INFO "TM => %u burst_request, ack %u\n", connection_id, tcph->ack_seq);
      print_packet_info(connection_id, tcph, iph, skb, DIR_OUT, "burst_request");
#endif
      
      newPacket(connection_id, tcp_payload, 0, true);
    }
    
    
    else if(connection->burst_stage == BURST_START)
    {
      if(connection->direction == 1)  // When this is a upload flow. We only send the real tcp traffic on the main direction
      {
        
#ifdef DISPLAY_BURST_STAGE
        //printk(KERN_INFO "TM => Sent another burst packet. With connection ID: %u and ACK value: %u. \n", connection_id, tcph->ack_seq);
        //printk(KERN_INFO "TM => %u burst, ack %u\n", connection_id, tcph->ack_seq);
        print_packet_info(connection_id, tcph, iph, skb, DIR_OUT, "burst");
#endif

        newPacket(connection_id, tcp_payload, 0, false);
      }
      
      else
      {


#ifdef DISPLAY_BURST_STAGE
        //printk(KERN_INFO "TM => This is just an ACK packet to the real burst packet on the download direction. With the connection ID: %u. Hence it is not part of the burst traffic.\n", connection_id);
        //printk(KERN_INFO "TM => %u ack_only, ack %u\n", connection_id, tcph->seq);
        print_packet_info(connection_id, tcph, iph, skb, DIR_OUT, "ack_only");
#endif
      }
    }

    
    else if(connection->burst_stage == SYN && tcph->ack)
    {
      connection->burst_stage = SYN_ACK;


#ifdef DISPLAY_BURST_STAGE
      //printk(KERN_INFO "TM => burst_stage changed to SYN-ACK. With connection ID: %u and ACK value: %u. Should not happen currently since mobile is not acting as server.\n", connection_id, tcph->ack_seq);
      //printk(KERN_INFO "TM => %u burst_synack, ack %u\n", connection_id, tcph->ack_seq);
      print_packet_info(connection_id, tcph, iph, skb, DIR_OUT, "burst_synack");
#endif
      
      //      connection->direction = 1;
      
      // TODO: Generate NEW_BURST event
    }   
  }
  
  //  printk(KERN_INFO "TM => OnChocking flag: %b. Connection chocked flag: %b. \n", OnChocking, connection->chocked);
  
  //  When there is a chocking flag and this connection has not been chocked
  if(OnChocking && !(connection->chocked))
  {
    connection->previous_window_size = connection->window_size;
    set_tcp_window_size(skb, tcph, iph, 0x00, 0);
  }

  // When there is an unchocking flag and this connection has been chocked
  if(!OnChocking && connection->chocked)
  {
    connection->previous_window_size = connection->window_size;
    set_tcp_window_size(skb, tcph, iph, connection->previous_window_size, connection->window_scale);
  }

#ifndef ONLY_SHOW_SYN_ACK
  return NF_ACCEPT;
#endif

}




static inline void set_tcp_window_size(struct sk_buff* my_skb, struct tcphdr* tcph, struct iphdr* iph, u_int32_t window_size, u_int16_t window_scale)
{

  u_int16_t tcplen;

  if (my_skb == NULL || tcph == NULL || iph == NULL || window_scale<0 || window_size < 0)
    return;

  tcplen = my_skb->len - ip_hdrlen(my_skb);

  /*
   * Set Window Size after scaling
   */
  tcph->window = htons(window_size>>window_scale);

  /*
   * Check sum
   */
  tcph->check = 0;
  tcph->check = tcp_v4_check(tcplen, iph->saddr, iph->daddr, csum_partial(
                                                                          (char *) tcph, tcplen, 0));

}


/**
 * Structures used for Hook Registration
 */

/**
 * hook_local_out
 *
 * Deals with with outgoing traffic from local processes.
 */
static struct nf_hook_ops hook_local_out_ops __read_mostly =
  { .pf = PF_INET, .priority = 1, .hooknum = NF_INET_POST_ROUTING,//NF_INET_LOCAL_OUT,
    .hook = hook_local_out, };

/**
 * hook_local_in
 *
 * Deals with with incoming traffic for local processes.
 */
static struct nf_hook_ops hook_local_in_ops = //__read_mostly =
  { .pf = PF_INET, .priority = NF_IP_PRI_LAST, .hooknum = NF_INET_LOCAL_IN,
    .hook = hook_local_in, };

#endif /* NFHOOKS_H_ */
