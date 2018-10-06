package ProjetJee.core.service.impl;

import ProjetJee.core.dao.CatDAO;
import ProjetJee.core.entity.Cat;
import ProjetJee.core.entity.User;
import ProjetJee.core.service.CatService;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

@Named
@Transactional
public class CatServiceImpl implements CatService {

    @Inject
    private CatDAO catDAO;

    public List<Cat> findByUser(User user) {
        return catDAO.findByUser(user);
    }
}