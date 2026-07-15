package com.dbdomino.moneylog.service;

import com.dbdomino.moneylog.dto.service.AmmountServiceDto;
import com.dbdomino.moneylog.mapper.AmmountMapper;
import com.dbdomino.moneylog.repository.itf.AmmountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AmmountService {
    private final AmmountRepository ammountRepository;
    private final AmmountMapper ammountMapper;

    public List<AmmountServiceDto> getAllAmmounts() {
        return ammountMapper.toServiceDtoList(ammountRepository.findAll());
    }
    public Optional<AmmountServiceDto> getAmmountById(Long id) {
        return ammountRepository.findById(id)
                .map(ammount -> ammountMapper.toServiceDto(ammount));
    }
    @Transactional
    public void  createOrUpdateAmmount(AmmountServiceDto ammountServiceDto) {
        ammountRepository.save(ammountMapper.toEntity(ammountServiceDto));
    }

    // Ammount 삭제
    @Transactional
    public void deleteAmmount(AmmountServiceDto ammountServiceDto) {
        ammountRepository.delete(ammountMapper.toEntity(ammountServiceDto));
    }

    public List<AmmountServiceDto> getAmmountsByMemberId(String memberId) {
        return ammountMapper.toServiceDtoList(ammountRepository.findByMemberId(memberId));
    }

}
