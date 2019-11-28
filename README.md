# Command-Control-Networking-Protocol
A Command &amp; Control Networking Protocol programmed in Java. A User may chose to send a message via a Client to a Broker which then forwards said message to a multitude of workers.

The program uses a synchronized series of methods to send a Datagram Packet to a series of Nodes. The Header of this Packet determines how the content of this packet may be processed; whether it's to decide a worker's name, the client's socket address, etc.
