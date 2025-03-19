package com.moko.support.ac.task;

import com.moko.ble.lib.task.OrderTask;
import com.moko.support.ac.entity.OrderCHAR;


public class GetSerialNumberTask extends OrderTask {

    public byte[] data;

    public GetSerialNumberTask() {
        super(OrderCHAR.CHAR_SERIAL_NUMBER, OrderTask.RESPONSE_TYPE_READ);
    }

    @Override
    public byte[] assemble() {
        return data;
    }
}
