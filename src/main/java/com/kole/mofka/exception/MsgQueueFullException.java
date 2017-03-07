package com.kole.mofka.exception;

/**
 * autor: yang.jianjun@immomo.com
 * created Date:2017-03-07
 */
public class MsgQueueFullException extends RuntimeException {
    public MsgQueueFullException(String message) {
        super(message);
    }
}
