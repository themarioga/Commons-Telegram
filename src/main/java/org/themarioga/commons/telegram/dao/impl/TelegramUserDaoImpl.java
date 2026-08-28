package org.themarioga.commons.telegram.dao.impl;

import org.springframework.stereotype.Repository;
import org.themarioga.commons.engine.dao.AbstractHibernateDao;
import org.themarioga.commons.engine.models.User;
import org.themarioga.commons.telegram.dao.intf.TelegramUserDao;
import org.themarioga.commons.telegram.models.TelegramUser;

@Repository
public class TelegramUserDaoImpl extends AbstractHibernateDao<TelegramUser> implements TelegramUserDao {

    public TelegramUserDaoImpl() {
        setClazz(TelegramUser.class);
    }

    @Override
    public TelegramUser getByIdFetchingUser(Long telegramId) {
        return getCurrentSession().createQuery("SELECT tu FROM TelegramUser tu JOIN FETCH tu.user u JOIN FETCH u.lang WHERE tu.id = :telegramId", TelegramUser.class).setParameter("telegramId", telegramId).getSingleResultOrNull();
    }

    @Override
    public TelegramUser getByUser(User user) {
        return getCurrentSession().createQuery("SELECT tu FROM TelegramUser tu WHERE tu.user = :user", TelegramUser.class).setParameter("user", user).getSingleResultOrNull();
    }

    @Override
    public void flush() {
        getEntityManager().flush();
    }

}
