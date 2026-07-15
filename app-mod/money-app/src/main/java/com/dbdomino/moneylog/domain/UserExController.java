//package com.dbdomino.moneylog.domain;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//
//public class UserExController {
//
//    @Autowired
//    private UserExRepository userExRepository;
//
//    @GetMapping
//    public List<UserEx> getUsers() {
//        return userExRepository.findAll(); // data jpa 자동완성
//    }
//
//    @GetMapping("/{id}")
//    public UserEx getUsersById(@PathVariable Long id) {
//        return userExRepository.findById(id).orElseThrow(() -> new RuntimeException("member id not found"));
//    }
//
//    @PostMapping
//    public UserEx createUsers(@RequestBody UserEx userEx) {
//        return userExRepository.save(userEx);
//    }
//
//    @PutMapping("/{id}")
//    public UserEx updateUsers(@PathVariable Long id, @RequestBody UserEx userExBody) {
//        UserEx mem1 = userExRepository.findById(id).orElseThrow(() -> new RuntimeException("member id not found"));
//
//        mem1.setName(userExBody.getName());
//        mem1.setEmail(userExBody.getEmail());
//
//        return userExRepository.save(mem1);
//    }
//
//    @DeleteMapping("/{id}")
//    public void deleteUsers(@PathVariable Long id) {
//        UserEx userEx = userExRepository.findById(id).orElseThrow(() -> new RuntimeException("member id not found"));
//        userExRepository.delete(userEx);
//    }
//}
