package com.example.apigateway.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.example.apigateway.model.RedisTokenInfo;

@Repository
public interface RedisTokenRepository extends CrudRepository<RedisTokenInfo, String> {

}