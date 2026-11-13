package com.osdiag;

/** A program waiting for connections on a port. */
public record Listener(int port, int pid) {
}
