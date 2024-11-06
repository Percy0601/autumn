package com.microapp.autumn.sample.boot.provider.service;

import java.util.List;

import org.apache.thrift.TException;

import com.microapp.autumn.api.annotation.Export;
import com.microapp.autumn.sample.api.SomeService;
import com.microapp.autumn.sample.api.User;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: baoxin.zhao
 * @date: 2024/10/8
 */
@Export
@Slf4j
public class BootProviderServiceImpl implements SomeService.Iface {
    @Override
    public String echo(String msg) throws TException {
        return "Hello boot provider, " + msg;
    }

    @Override
    public int addUser(User user) throws TException {
        return 99;
    }

    @Override
    public List<User> findUserByIds(List<Integer> idList) throws TException {
        return List.of();
    }
}
