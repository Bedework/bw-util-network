package org.bedework.util.servlet;

/** In the absence of a conversation parameter we assume that a
 * conversation starts with actionType=action and ends with
 * actionType=render.
 * <p>
 * Conversations are related to the persistence framework and
 * allow us to keep a persistence engine session running until
 * the sequence of actions is completed.
 */
public interface ConversationTypes {
  int conversationTypeUnknown = 0;

  /** start of a multi-request conversation */
  int conversationTypeStart = 1;

  /** part-way through a multi-request conversation */
  int conversationTypeContinue = 2;

  /** end of a multi-request conversation */
  int conversationTypeEnd = 3;

  /** if a conversation is started, end it on entry with no
   * processing of changes. Start a new conversation which we will end on exit.
   */
  int conversationTypeOnly = 4;

  /** If a conversation is already started on entry, process changes and end it.
   * Start a new conversation which we will end on exit.
   */
  int conversationTypeProcessAndOnly = 5;

  /** */
  String[] conversationTypes = {"unknown",
                                                    "start",
                                                    "continue",
                                                    "end",
                                                    "only",
                                                    "processAndOnly"};
}
