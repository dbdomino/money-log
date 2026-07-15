package com.dbdomino.moneylog.controller;

import com.dbdomino.moneylog.dto.response.AmmountResponseDto;
import com.dbdomino.moneylog.mapper.AmmountMapper;
import com.dbdomino.moneylog.service.AmmountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ammounts")
public class AmmountController {
    private final AmmountService ammountService;
    private final RestClient restClient = RestClient.builder().build();
    private final AmmountMapper ammountMapper;

    @GetMapping
    public List<AmmountResponseDto> getAllAmmounts() {
        return ammountMapper.toResponseDtoList(ammountService.getAllAmmounts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AmmountResponseDto> getAmmountById(@PathVariable Long id) {
        return ammountService.getAmmountById(id)
                .map(serviceDto -> ResponseEntity.ok().body(ammountMapper.toResponseDto(serviceDto)) ) // Optional이 비어 있지 않다면 200 OK와 함께 데이터 반환
                .orElseGet(() -> ResponseEntity.notFound().build());  // Optional이 비어 있다면 404 Not Found 반환
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<AmmountResponseDto>> getAmmountByMemberId(@PathVariable String memberId) {
        return ResponseEntity.ok().body(ammountMapper.toResponseDtoList(ammountService.getAmmountsByMemberId(memberId)));
//        return ammountList.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(ammountList);
//        if (ammountList.isEmpty()) {
//            return ResponseEntity.notFound().build();
//        }
//        return ResponseEntity.ok(ammountList);
    }
}
