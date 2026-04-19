package com.example.accountservice.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.example.accountservice.model.RedisTokenInfo;

@Repository
public interface RedisTokenRepository extends CrudRepository<RedisTokenInfo, String> {

}