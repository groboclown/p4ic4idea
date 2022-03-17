/**
 * Manages the client list and the server connection state.
 * <p>
 * This changes the client list based on: initialization of the component (loaded through the clientconfig module);
 * and the messages sent from the clientconfig module.
 * <p>
 * Messages used by this module are split into three categories:
 * <ul>
 *     <li>Project Configuration Updates - listening to changes to the configuration from the clientconfig.</li>
 *     <li>Server Connection State Changes - listening to whether a server can connect, or reasons why it can't
 *     connect.  These are application-wide ... should they be?</li>
 *     <li>Changes in clients - as the configuration state is updated, that can make client connections become
 *     removed or added.</li>
 * </ul>
 */
package net.groboclown.p4plugin.modules.connection;
