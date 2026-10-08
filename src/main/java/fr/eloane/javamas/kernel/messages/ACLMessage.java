/* 
 * The MIT License
 *
 * Copyright 2018 Guillaume Monet.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package fr.eloane.javamas.kernel.messages;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Original Work by the MaDKit Team : message send by Agents
 */
public final class ACLMessage extends Message<String> {

    /**
     * constant identifying the FIPA performative *
     */
    public static final int ACCEPT_PROPOSAL = 0;
    /**
     * constant identifying the FIPA performative *
     */
    public static final int AGREE = 1;
    /**
     * constant identifying the FIPA performative *
     */
    public static final int CANCEL = 2;
    /**
     * constant identifying the FIPA performative *
     */
    public static final int CFP = 3;
    /**
     * constant identifying the FIPA performative *
     */
    public static final int CONFIRM = 4;
    /**
     * constant identifying the FIPA performative *
     */
    public static final int DISCONFIRM = 5;
    /**
     * constant identifying the FIPA performative *
     */
    public static final int FAILURE = 6;
    /**
     * constant identifying the FIPA performative *
     */
    public static final int INFORM = 7;
    /**
     * constant identifying the FIPA performative *
     */
    public static final int INFORM_IF = 8;
    /**
     * constant identifying the FIPA performative *
     */
    public static final int INFORM_REF = 9;
    /**
     * constant identifying the FIPA performative *
     */
    public static final int NOT_UNDERSTOOD = 10;
    /**
     * constant identifying the FIPA performative *
     */
    public static final int PROPOSE = 11;
    /**
     * constant identifying the FIPA performative *
     */
    public static final int QUERY_IF = 12;
    /**
     * constant identifying the FIPA performative *
     */
    public static final int QUERY_REF = 13;
    /**
     * constant identifying the FIPA performative *
     */
    public static final int REFUSE = 14;
    /**
     * constant identifying the FIPA performative *
     */
    public static final int REJECT_PROPOSAL = 15;
    /**
     * constant identifying the FIPA performative *
     */
    public static final int REQUEST = 16;
    /**
     * constant identifying the FIPA performative *
     */
    public static final int REQUEST_WHEN = 17;
    /**
     * constant identifying the FIPA performative *
     */
    public static final int REQUEST_WHENEVER = 18;
    /**
     * constant identifying the FIPA performative *
     */
    public static final int SUBSCRIBE = 19;
    /**
     * constant identifying the FIPA performative *
     */
    public static final int PROXY = 20;
    /**
     * constant identifying the FIPA performative *
     */
    public static final int PROPAGATE = 21;
    /**
     * constant identifying an unknown performative *
     */
    public static final int UNKNOWN = -1;
    /**
     *
     */
    public static final String ACCEPT_PROPOSAL_STRING = "ACCEPT-PROPOSAL";
    /**
     *
     */
    public static final String AGREE_STRING = "AGREE";
    /**
     *
     */
    public static final String CANCEL_STRING = "CANCEL";
    /**
     *
     */
    public static final String CFP_STRING = "CFP";
    /**
     *
     */
    public static final String CONFIRM_STRING = "CONFIRM";
    /**
     *
     */
    public static final String DISCONFIRM_STRING = "DISCONFIRM";
    /**
     *
     */
    public static final String FAILURE_STRING = "FAILURE";
    /**
     *
     */
    public static final String INFORM_STRING = "INFORM";
    /**
     *
     */
    public static final String INFORM_IF_STRING = "INFORM-IF";
    /**
     *
     */
    public static final String INFORM_REF_STRING = "INFORM-REF";
    /**
     *
     */
    public static final String NOT_UNDERSTOOD_STRING = "NOT-UNDERSTOOD";
    /**
     *
     */
    public static final String PROPOSE_STRING = "PROPOSE";
    /**
     *
     */
    public static final String QUERY_IF_STRING = "QUERY-IF";
    /**
     *
     */
    public static final String QUERY_REF_STRING = "QUERY-REF";
    /**
     *
     */
    public static final String REFUSE_STRING = "REFUSE";
    /**
     *
     */
    public static final String REJECT_PROPOSAL_STRING = "REJECT-PROPOSAL";
    /**
     *
     */
    public static final String REQUEST_STRING = "REQUEST";
    /**
     *
     */
    public static final String REQUEST_WHEN_STRING = "REQUEST-WHEN";
    /**
     *
     */
    public static final String REQUEST_WHENEVER_STRING = "REQUEST-WHENEVER";
    /**
     *
     */
    public static final String SUBSCRIBE_STRING = "SUBSCRIBE";
    /**
     *
     */
    public static final String PROXY_STRING = "PROXY";
    /**
     *
     */
    public static final String PROPAGATE_STRING = "PROPAGATE";
    /**
     *
     */
    /**
     * The performatives, indexed by their constant
     */
    public static final ArrayList<String> PERFORMATIVES = new ArrayList<>(List.of(
            ACCEPT_PROPOSAL_STRING,
            AGREE_STRING,
            CANCEL_STRING,
            CFP_STRING,
            CONFIRM_STRING,
            DISCONFIRM_STRING,
            FAILURE_STRING,
            INFORM_STRING,
            INFORM_IF_STRING,
            INFORM_REF_STRING,
            NOT_UNDERSTOOD_STRING,
            PROPOSE_STRING,
            QUERY_IF_STRING,
            QUERY_REF_STRING,
            REFUSE_STRING,
            REJECT_PROPOSAL_STRING,
            REQUEST_STRING,
            REQUEST_WHEN_STRING,
            REQUEST_WHENEVER_STRING,
            SUBSCRIBE_STRING,
            PROXY_STRING,
            PROPAGATE_STRING));
    @Serial
    private static final long serialVersionUID = -2112713185673830635L;

    private String action;

    /**
     * Default constructor for ACLMessage class
     */
    public ACLMessage() {
        action = NOT_UNDERSTOOD_STRING;
        content = "NO_CONTENT";
    }

    /**
     * Constructor for ACLMessage class
     *
     * @param actType the performative
     */
    public ACLMessage(String actType) {
        action = actType.toUpperCase(Locale.ROOT);
    }

    /**
     * Constructor for ACLMessage class
     *
     * @param actType the performative
     * @param cont the content of the message
     */
    public ACLMessage(String actType, String cont) {
        action = actType.toUpperCase(Locale.ROOT);
        content = cont;
    }

    /**
     * Constructor for ACLMessage class
     *
     * @param perf a performative as an integer
     * @param cont the content of the message
     */
    public ACLMessage(int perf, String cont) {
        this.action = perf >= 0 && perf < PERFORMATIVES.size() ? PERFORMATIVES.get(perf) : "UNKNOWN";
        this.content = cont;
    }

    /**
     * return the performative
     *
     * @return String
     */
    public String getAct() {
        return action;
    }

    /**
     * @return
     */
    public String getPerformative() {
        return getAct();
    }

    /**
     * @return
     */
    public String getValue() {
        return getContent();
    }

    /**
     * @param action
     */
    public void setPerformative(String action) {
        this.action = action;
    }

    /**
     * @return
     */
    @Override
    public String toString() {
        return action + " " + content;
    }
}
