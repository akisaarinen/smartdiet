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

#include <linux/module.h>
#include <linux/kernel.h>
#include <linux/init.h>
#include <linux/sched.h>
#include <linux/netlink.h>
#include <linux/time.h>
#include <net/sock.h>
#include <net/net_namespace.h>


/**
 * Kernel - User Communication Include Files
 */

#include <asm/siginfo.h>	//siginfo
#include <linux/rcupdate.h>	//rcu_read_lock
#include <linux/sched.h>	//find_task_by_pid_type << obsolete..
#include <linux/delay.h>	// void msleep(unsigned int sleep_time);
#include <linux/debugfs.h>
#include <linux/uaccess.h>

/**
 * Wireless Extensions
 */
#include <linux/netdevice.h>
#include <net/iw_handler.h>
#include <linux/netdevice.h>

/**
 * Local includes
 */
#include "ec.h"
#include "connections.h"
#include "nfhooks.h"

#define MAX_PAYLOAD 2000

static struct sock *nl_sk = NULL;
static int pid;
static bool gotPID = false;

static void processRequest (struct sk_buff *skb)
{  
  struct nlmsghdr *nlh = NULL;
  if(skb == NULL)
  {
    printk("TM: skb is NULL \n");
    return ;
  }
  
  nlh = (struct nlmsghdr *)skb->data;
  printk(KERN_INFO "TM: %s: received netlink message payload: %s\n", __FUNCTION__, NLMSG_DATA(nlh));
  
  char *payload = (char*)NLMSG_DATA(nlh);

  // The request type.
  // 0 means initial request.
  // 1 means set window size zero request.
  // 2 means restore window size request
  int request;


  sscanf(payload, "%d", &request);

  if(0 == request)
  {
  // 0 means this is an initial message sent from user space program to indicate the PID
    nlh=(struct nlmsghdr*)skb->data;
    pid = nlh->nlmsg_pid; /*pid of sending process */
    printk(KERN_INFO "TM: Received user space initial message. The PID is %d \n", pid);
    gotPID = true;
  }
  else if(1 == request)
  {     //  case 1: // 1 means this is a request to set the windows size to zero
    OnChocking = true;
    printk(KERN_INFO "Window Size: Received user space request to set windows size to zero\n");
  }
  
  else if(2 == request)
  {     //  case 2: // 2 means this is a request to recover the windows size to previous values
    OnChocking = false;
    printk(KERN_INFO "Window Size: Received user space request to recover the windows size\n");
  }
  
  else
    printk(KERN_ERR "TM: Invalid request type with value\n");
}


void newPacket(u_int32_t connection_id, unsigned int size, int direction, bool newConnection)
{

  if( direction>1 || direction<0 )
  {
    printk(KERN_ERR "TM: packet direction error\n");
    return;
  }
  
  if(gotPID)
  {   
    // To send the message back to user space application
    struct nlmsghdr *nlh = NULL;
    struct skb_buff *skb_out;
    int msg_size;

    struct timeval tv;
    //    struct tm *tm;
    do_gettimeofday(&tv);
 
    char temp[30];
    char temp2[10];
    char temp3[10];
    char temp4[10];
    char temp5[10];
    
    char msg[MAX_PAYLOAD];
      
    // The message should be in such format:
    // eventType,packetInterval,newConnection,direction,connection_id,packetsize
    // eventType: integer, and 2 means it is a new packet event.
    // packetInterval: unsigned long indicating the packet interval since last burst packet in microseconds.
    // newConnection: integer. 1 means this is a new connection, and 0 means this is an already existing connection.
    // direction: integer. 0 means uplink, 1 means downlink.
    // connection_id: unsigned integer.
    // packetsize: unsigned integer
    

    sprintf(msg, "%d", 2);
    strcat(msg, ",");

    unsigned long timeCurrent = tv.tv_sec*1000000+tv.tv_usec;
    
    struct constate *connection = get_connection(&all_connections_head, connection_id);
    unsigned long packetInterval = timeCurrent-connection->timeStamp;
    connection->timeStamp = timeCurrent;

    sprintf(temp, "%u", packetInterval);
    strcat(msg, temp);
    
    strcat(msg, ",");
    
    if(newConnection)
    {
      strcat(msg, "1");
    }
    
    else
    {
      strcat(msg, "0");
    }
    strcat(msg, ",");
    sprintf(temp3, "%d", direction);
    strcat(msg, temp3);
    strcat(msg, ",");
    sprintf(temp4, "%u", connection_id);
    strcat(msg, temp4);
    strcat(msg, ",");    sprintf(temp5, "%d", size);
    strcat(msg, temp5);
    int res;
    msg_size=strlen(msg);
    skb_out = nlmsg_new(msg_size,0);
    
    if(!skb_out)
    {
      printk(KERN_ERR "TM: Failed to allocate new skb");
      return;
    } 
    nlh=nlmsg_put(skb_out,0,0,NLMSG_DONE,msg_size,0);
    strncpy(nlmsg_data(nlh),msg,msg_size);
    res=nlmsg_unicast(nl_sk,skb_out,pid);

    printk(KERN_INFO "TCP packet: Msg to be sent: %s.\n", msg);
    if(res<0)
      printk(KERN_ERR "TCP packet: Error while sending back to user.\n\n");
    else
      printk(KERN_INFO "TCP packet: Sending new packet message successfully. Msg: %s.\n\n", msg);    
  }
}


static void netlink_create()
{
   nl_sk = netlink_kernel_create(&init_net, NETLINK_TRAFFICMONITOR,0, processRequest, NULL, THIS_MODULE);
  if (nl_sk == NULL)
    printk(KERN_ERR "netlink: Fail in Initializing Netlink Socket");
  else
    printk(KERN_INFO "Initializing Netlink Socket successfully");
}



/**
 * Initialize module
 *
 */
static int __init ec_module_init(void)
{
  printk(KERN_INFO "TM :: MODULE INITIALIZATION, timestamp %llu\n", gettime());

  printk(KERN_INFO "netlink: Initializing Netlink Socket\n");
  netlink_create();
    
  /**
   * Connection Tracking
   */

  all_connections_head=NULL;
  spin_lock_init( &all_connections_spinlock);


  /**
   * Hook Registeration
   */
  //nf_register_hook(&myhook_ops); // temporary hook
  nf_register_hook(&hook_local_out_ops); // local out
  nf_register_hook(&hook_local_in_ops); // local in

  printk(KERN_INFO "TM :: MODULE ENABLED\n" );

  return 0;

}

/**
 * Exit/Clean-up module
 *
 */
static int __exit ec_module_exit(void)
{

  nf_unregister_hook(&hook_local_out_ops);
  nf_unregister_hook(&hook_local_in_ops);
  delete_all(&all_connections_head);
  sock_release(nl_sk->sk_socket);
  printk(KERN_INFO "TM :: MODULE DISABLED \n");
  return 0;
}

module_init(ec_module_init);
module_exit(ec_module_exit);
MODULE_LICENSE("GPL");
MODULE_AUTHOR("Ahmad Nazir, Mohammad Hoque, Wei Li, Aki Saarinen");

