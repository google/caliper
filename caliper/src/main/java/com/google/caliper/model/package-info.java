/**
 * These classes model the data that is collected by the caliper {@linkplain
 * com.google.caliper.runner runner}: the record of which scenarios were tested on which VMs by
 * which instruments and, most importantly, all the measurements that were observed. The goal of
 * these classes is to be as easily convertible back and forth to JSON text as possible.
 *
 * <p>We've kept them very quick-and-dirty for now (public mutable fields!?!), but may buff them up
 * after things stabilize.
 */
package com.google.caliper.model;