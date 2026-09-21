CREATE TABLE IF NOT EXISTS "customer_profile" (
    -- Автоинкрементный ID
                                                  "id" BIGSERIAL NOT NULL,
    -- UUID пользователя из Keycloak
                                                  "keycloak_user_id" VARCHAR(255) NOT NULL UNIQUE,
    -- Email (уникальный логин)
                                                  "email" VARCHAR(255) NOT NULL UNIQUE,
    -- Телефон для связи (уникальный)
                                                  "phone" VARCHAR(20) UNIQUE,
    -- Имя
                                                  "first_name" VARCHAR(100) NOT NULL,
    -- Фамилия
                                                  "last_name" VARCHAR(100) NOT NULL,
    -- Отчество (опционально)
                                                  "middle_name" VARCHAR(100),
    -- Дата рождения
                                                  "birth_date" DATE,
    -- Предпочитаемый язык: ru, en
                                                  "preferred_language" VARCHAR(10) NOT NULL DEFAULT 'ru',
    -- Дата создания профиля
                                                  "created_at" TIMESTAMP NOT NULL DEFAULT NOW(),
    -- Дата последнего обновления
                                                  "updated_at" TIMESTAMP NOT NULL DEFAULT NOW(),
                                                  PRIMARY KEY("id")
);

COMMENT ON TABLE "customer_profile" IS 'Профиль пользователя (бизнес-данные, не аутентификация)';
COMMENT ON COLUMN "customer_profile"."id" IS 'Автоинкрементный ID';
COMMENT ON COLUMN "customer_profile"."keycloak_user_id" IS 'UUID пользователя из Keycloak';
COMMENT ON COLUMN "customer_profile"."email" IS 'Email (уникальный логин)';
COMMENT ON COLUMN "customer_profile"."phone" IS 'Телефон для связи (уникальный)';
COMMENT ON COLUMN "customer_profile"."first_name" IS 'Имя';
COMMENT ON COLUMN "customer_profile"."last_name" IS 'Фамилия';
COMMENT ON COLUMN "customer_profile"."middle_name" IS 'Отчество (опционально)';
COMMENT ON COLUMN "customer_profile"."birth_date" IS 'Дата рождения';
COMMENT ON COLUMN "customer_profile"."preferred_language" IS 'Предпочитаемый язык: ru, en';
COMMENT ON COLUMN "customer_profile"."created_at" IS 'Дата создания профиля';
COMMENT ON COLUMN "customer_profile"."updated_at" IS 'Дата последнего обновления';

CREATE UNIQUE INDEX "idx_customer_profile_keycloak_id"
    ON "customer_profile" ("keycloak_user_id");
CREATE UNIQUE INDEX "idx_customer_profile_email"
    ON "customer_profile" ("email");
CREATE UNIQUE INDEX "idx_customer_profile_phone"
    ON "customer_profile" ("phone");

CREATE TABLE IF NOT EXISTS "delivery_address" (
    -- Первичный ключ
                                                  "id" BIGSERIAL NOT NULL,
    -- Владелец адреса (ссылка на customer_profile.id)
                                                  "customer_id" BIGINT NOT NULL,
    -- Метка адреса (Дом, Работа, Дача)
                                                  "alias" VARCHAR(50),
    -- Страна
                                                  "country" VARCHAR(100) NOT NULL,
    -- Город
                                                  "city" VARCHAR(100) NOT NULL,
    -- Улица
                                                  "street" VARCHAR(255) NOT NULL,
    -- Номер дома
                                                  "house" VARCHAR(20) NOT NULL,
    -- Номер квартиры/офиса
                                                  "apartment" VARCHAR(20),
    -- Почтовый индекс
                                                  "postal_code" VARCHAR(20),
    -- Признак адреса по умолчанию
                                                  "is_default" BOOLEAN NOT NULL DEFAULT FALSE,
    -- Дата создания
                                                  "created_at" TIMESTAMP NOT NULL DEFAULT NOW(),
    -- Дата последнего обновления
                                                  "updated_at" TIMESTAMP NOT NULL DEFAULT NOW(),
                                                  PRIMARY KEY("id")
);

COMMENT ON TABLE "delivery_address" IS 'Адреса доставки пользователей';
COMMENT ON COLUMN "delivery_address"."id" IS 'Первичный ключ';
COMMENT ON COLUMN "delivery_address"."customer_id" IS 'Владелец адреса (ссылка на customer_profile.id)';
COMMENT ON COLUMN "delivery_address"."alias" IS 'Метка адреса (Дом, Работа, Дача)';
COMMENT ON COLUMN "delivery_address"."country" IS 'Страна';
COMMENT ON COLUMN "delivery_address"."city" IS 'Город';
COMMENT ON COLUMN "delivery_address"."street" IS 'Улица';
COMMENT ON COLUMN "delivery_address"."house" IS 'Номер дома';
COMMENT ON COLUMN "delivery_address"."apartment" IS 'Номер квартиры/офиса';
COMMENT ON COLUMN "delivery_address"."postal_code" IS 'Почтовый индекс';
COMMENT ON COLUMN "delivery_address"."is_default" IS 'Признак адреса по умолчанию';
COMMENT ON COLUMN "delivery_address"."created_at" IS 'Дата создания';
COMMENT ON COLUMN "delivery_address"."updated_at" IS 'Дата последнего обновления';

CREATE INDEX "idx_delivery_address_customer_default"
    ON "delivery_address" ("customer_id", "is_default");
CREATE INDEX "idx_delivery_address_customer_id"
    ON "delivery_address" ("customer_id");

CREATE TABLE IF NOT EXISTS "payment_method" (
    -- Первичный ключ
                                                "id" BIGSERIAL NOT NULL,
    -- Владелец способа оплаты (ссылка на customer_profile.id)
                                                "customer_id" BIGINT NOT NULL,
    -- Тип: CARD, WALLET, GOOGLE_PAY, APPLE_PAY
                                                "type" VARCHAR(255) NOT NULL,
    -- Маскированный номер карты (**** 1234) — только для CARD
                                                "masked_pan" VARCHAR(255),
    -- Токен платежного шлюза (для списания)
                                                "token" VARCHAR(255) NOT NULL,
    -- Срок действия (только для карт)
                                                "expiry_date" DATE,
    -- Признак способа оплаты по умолчанию (только один TRUE на пользователя)
                                                "is_default" BOOLEAN NOT NULL DEFAULT FALSE,
    -- Дата создания
                                                "created_at" TIMESTAMP NOT NULL DEFAULT NOW(),
    -- Дата последнего обновления
                                                "updated_at" TIMESTAMP NOT NULL DEFAULT NOW(),
                                                PRIMARY KEY("id")
);

COMMENT ON TABLE "payment_method" IS 'Способы оплаты пользователей (сохраненные карты, кошельки)';
COMMENT ON COLUMN "payment_method"."id" IS 'Первичный ключ';
COMMENT ON COLUMN "payment_method"."customer_id" IS 'Владелец способа оплаты (ссылка на customer_profile.id)';
COMMENT ON COLUMN "payment_method"."type" IS 'Тип: CARD, WALLET, GOOGLE_PAY, APPLE_PAY';
COMMENT ON COLUMN "payment_method"."masked_pan" IS 'Маскированный номер карты (**** 1234) — только для CARD';
COMMENT ON COLUMN "payment_method"."token" IS 'Токен платежного шлюза (для списания)';
COMMENT ON COLUMN "payment_method"."expiry_date" IS 'Срок действия (только для карт)';
COMMENT ON COLUMN "payment_method"."is_default" IS 'Признак способа оплаты по умолчанию (только один TRUE на пользователя)';
COMMENT ON COLUMN "payment_method"."created_at" IS 'Дата создания';
COMMENT ON COLUMN "payment_method"."updated_at" IS 'Дата последнего обновления';

CREATE INDEX "idx_payment_method_customer_default"
    ON "payment_method" ("customer_id", "is_default");
CREATE INDEX "idx_payment_method_customer_id"
    ON "payment_method" ("customer_id");

CREATE TABLE IF NOT EXISTS "product" (
    -- Первичный ключ
                                         "id" BIGSERIAL NOT NULL,
    -- Название товара
                                         "name" VARCHAR(255) NOT NULL,
    -- Описание товара
                                         "description" TEXT,
    -- Текущая цена
                                         "price" DECIMAL(10,2) NOT NULL,
    -- Старая цена (для отображения скидки)
                                         "old_price" DECIMAL(10,2),
    -- Ссылка на категорию товара
                                         "category_id" BIGINT,
    -- Бренд производитель
                                         "brand" VARCHAR(100),
    -- Артикул товара (уникальный)
                                         "sku" VARCHAR(50) UNIQUE,
    -- Доступен для продажи
                                         "is_active" BOOLEAN NOT NULL DEFAULT TRUE,
    -- Дата создания
                                         "created_at" TIMESTAMP NOT NULL DEFAULT NOW(),
    -- Дата последнего обновления
                                         "updated_at" TIMESTAMP NOT NULL DEFAULT NOW(),
                                         PRIMARY KEY("id")
);

COMMENT ON TABLE "product" IS 'Товары в каталоге интернет-магазина';
COMMENT ON COLUMN "product"."id" IS 'Первичный ключ';
COMMENT ON COLUMN "product"."name" IS 'Название товара';
COMMENT ON COLUMN "product"."description" IS 'Описание товара';
COMMENT ON COLUMN "product"."price" IS 'Текущая цена';
COMMENT ON COLUMN "product"."old_price" IS 'Старая цена (для отображения скидки)';
COMMENT ON COLUMN "product"."category_id" IS 'Ссылка на категорию товара';
COMMENT ON COLUMN "product"."brand" IS 'Бренд производитель';
COMMENT ON COLUMN "product"."sku" IS 'Артикул товара (уникальный)';
COMMENT ON COLUMN "product"."is_active" IS 'Доступен для продажи';
COMMENT ON COLUMN "product"."created_at" IS 'Дата создания';
COMMENT ON COLUMN "product"."updated_at" IS 'Дата последнего обновления';

CREATE INDEX "idx_product_name"
    ON "product" ("name");
CREATE INDEX "idx_product_category_id"
    ON "product" ("category_id");
CREATE INDEX "idx_product_sku"
    ON "product" ("sku");
CREATE INDEX "idx_product_is_active"
    ON "product" ("is_active");
CREATE INDEX "idx_product_price"
    ON "product" ("price");

CREATE TABLE IF NOT EXISTS "category" (
    -- Первичный ключ
                                          "id" BIGSERIAL NOT NULL,
    -- Название категории
                                          "name" VARCHAR(100) NOT NULL,
    -- Описание категории
                                          "description" TEXT,
    -- Родительская категория (NULL = корневая)
                                          "parent_id" BIGINT,
    -- URL-идентификатор категории (уникальный)
                                          "slug" VARCHAR(100) NOT NULL UNIQUE,
    -- Доступна для отображения
                                          "is_active" BOOLEAN NOT NULL DEFAULT true,
    -- Дата создания
                                          "created_at" TIMESTAMP NOT NULL DEFAULT NOW(),
    -- Дата последнего обновления
                                          "updated_at" TIMESTAMP NOT NULL DEFAULT NOW(),
                                          PRIMARY KEY("id")
);

COMMENT ON TABLE "category" IS 'Категории товаров (иерархическая структура)';
COMMENT ON COLUMN "category"."id" IS 'Первичный ключ';
COMMENT ON COLUMN "category"."name" IS 'Название категории';
COMMENT ON COLUMN "category"."description" IS 'Описание категории';
COMMENT ON COLUMN "category"."parent_id" IS 'Родительская категория (NULL = корневая)';
COMMENT ON COLUMN "category"."slug" IS 'URL-идентификатор категории (уникальный)';
COMMENT ON COLUMN "category"."is_active" IS 'Доступна для отображения';
COMMENT ON COLUMN "category"."created_at" IS 'Дата создания';
COMMENT ON COLUMN "category"."updated_at" IS 'Дата последнего обновления';

CREATE UNIQUE INDEX "idx_category_slug"
    ON "category" ("slug");
CREATE INDEX "idx_category_parent_id"
    ON "category" ("parent_id");
CREATE INDEX "idx_category_name"
    ON "category" ("name");
CREATE INDEX "idx_category_is_active"
    ON "category" ("is_active");

CREATE TABLE IF NOT EXISTS "cart" (
    -- Первичный ключ
                                      "id" BIGSERIAL NOT NULL,
    -- Владелец корзины (ссылка на customer_profile.id)
                                      "customer_id" BIGINT NOT NULL,
    -- Товар в корзине (ссылка на product.id)
                                      "product_id" BIGINT NOT NULL,
    -- Количество товара
                                      "quantity" INTEGER NOT NULL DEFAULT 1,
    -- Цена на момент добавления (чтобы не менялась при изменении цены товара)
                                      "price_snapshot" DECIMAL(10,2) NOT NULL,
    -- Дата добавления в корзину
                                      "created_at" TIMESTAMP NOT NULL DEFAULT NOW(),
    -- Дата последнего обновления
                                      "updated_at" TIMESTAMP NOT NULL DEFAULT NOW(),
                                      PRIMARY KEY("id")
);

COMMENT ON TABLE "cart" IS 'Корзина покупок пользователей';
COMMENT ON COLUMN "cart"."id" IS 'Первичный ключ';
COMMENT ON COLUMN "cart"."customer_id" IS 'Владелец корзины (ссылка на customer_profile.id)';
COMMENT ON COLUMN "cart"."product_id" IS 'Товар в корзине (ссылка на product.id)';
COMMENT ON COLUMN "cart"."quantity" IS 'Количество товара';
COMMENT ON COLUMN "cart"."price_snapshot" IS 'Цена на момент добавления (чтобы не менялась при изменении цены товара)';
COMMENT ON COLUMN "cart"."created_at" IS 'Дата добавления в корзину';
COMMENT ON COLUMN "cart"."updated_at" IS 'Дата последнего обновления';

CREATE INDEX "idx_cart_customer_id"
    ON "cart" ("customer_id");
CREATE INDEX "idx_cart_product_id"
    ON "cart" ("product_id");
CREATE UNIQUE INDEX "idx_cart_customer_product"
    ON "cart" ("customer_id", "product_id");

CREATE TABLE IF NOT EXISTS "orders" (
    -- Первичный ключ
                                        "id" BIGSERIAL NOT NULL,
    -- Покупатель (ссылка на customer_profile.id)
                                        "customer_id" BIGINT NOT NULL,
    -- Человекочитаемый номер заказа (уникальный)
                                        "order_number" VARCHAR(50) NOT NULL UNIQUE,
    -- Итоговая сумма заказа
                                        "total_amount" DECIMAL(10,2) NOT NULL,
    -- Статус заказа: CREATED, PAYMENT_PENDING, PAID, CONFIRMED, SHIPPED, DELIVERED, CANCELLED, REFUNDED
                                        "status" VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    -- Адрес доставки (ссылка на delivery_address.id)
                                        "delivery_address_id" BIGINT NOT NULL,
    -- Слепок адреса на момент заказа (JSON)
                                        "delivery_address_snapshot" TEXT,
    -- Способ оплаты (ссылка на payment_method.id)
                                        "payment_method_id" BIGINT NOT NULL,
    -- Способ доставки: COURIER, PICKUP, POST
                                        "delivery_method" VARCHAR(20) NOT NULL,
    -- Дата/время доставки
                                        "delivery_date" TIMESTAMP,
    -- Комментарий к заказу
                                        "comment" TEXT,
    -- Дата создания
                                        "created_at" TIMESTAMP NOT NULL DEFAULT NOW(),
    -- Дата последнего обновления
                                        "updated_at" TIMESTAMP NOT NULL DEFAULT NOW(),
                                        PRIMARY KEY("id")
);

COMMENT ON TABLE "orders" IS 'Заказы пользователей';
COMMENT ON COLUMN "orders"."id" IS 'Первичный ключ';
COMMENT ON COLUMN "orders"."customer_id" IS 'Покупатель (ссылка на customer_profile.id)';
COMMENT ON COLUMN "orders"."order_number" IS 'Человекочитаемый номер заказа (уникальный)';
COMMENT ON COLUMN "orders"."total_amount" IS 'Итоговая сумма заказа';
COMMENT ON COLUMN "orders"."status" IS 'Статус заказа: CREATED, PAYMENT_PENDING, PAID, CONFIRMED, SHIPPED, DELIVERED, CANCELLED, REFUNDED';
COMMENT ON COLUMN "orders"."delivery_address_id" IS 'Адрес доставки (ссылка на delivery_address.id)';
COMMENT ON COLUMN "orders"."delivery_address_snapshot" IS 'Слепок адреса на момент заказа (JSON)';
COMMENT ON COLUMN "orders"."payment_method_id" IS 'Способ оплаты (ссылка на payment_method.id)';
COMMENT ON COLUMN "orders"."delivery_method" IS 'Способ доставки: COURIER, PICKUP, POST';
COMMENT ON COLUMN "orders"."delivery_date" IS 'Дата/время доставки';
COMMENT ON COLUMN "orders"."comment" IS 'Комментарий к заказу';
COMMENT ON COLUMN "orders"."created_at" IS 'Дата создания';
COMMENT ON COLUMN "orders"."updated_at" IS 'Дата последнего обновления';

CREATE INDEX "idx_orders_customer_id"
    ON "orders" ("customer_id");
CREATE UNIQUE INDEX "idx_orders_order_number"
    ON "orders" ("order_number");
CREATE INDEX "idx_orders_status"
    ON "orders" ("status");
CREATE INDEX "idx_orders_created_at"
    ON "orders" ("created_at");
CREATE INDEX "idx_orders_customer_status"
    ON "orders" ("customer_id", "status");

CREATE TABLE IF NOT EXISTS "order_item" (
    -- Первичный ключ
                                            "id" BIGSERIAL NOT NULL,
    -- Заказ (ссылка на orders.id)
                                            "order_id" BIGINT NOT NULL,
    -- Товар (ссылка на product.id)
                                            "product_id" BIGINT NOT NULL,
    -- Название товара на момент заказа (слепок)
                                            "product_name" VARCHAR(255) NOT NULL,
    -- Количество товара
                                            "quantity" INTEGER NOT NULL,
    -- Цена на момент заказа (слепок)
                                            "price" DECIMAL(10,2) NOT NULL,
    -- Дата создания
                                            "created_at" TIMESTAMP NOT NULL DEFAULT NOW(),
                                            PRIMARY KEY("id")
);

COMMENT ON TABLE "order_item" IS 'Товары в заказе (слепки данных на момент заказа)';
COMMENT ON COLUMN "order_item"."id" IS 'Первичный ключ';
COMMENT ON COLUMN "order_item"."order_id" IS 'Заказ (ссылка на orders.id)';
COMMENT ON COLUMN "order_item"."product_id" IS 'Товар (ссылка на product.id)';
COMMENT ON COLUMN "order_item"."product_name" IS 'Название товара на момент заказа (слепок)';
COMMENT ON COLUMN "order_item"."quantity" IS 'Количество товара';
COMMENT ON COLUMN "order_item"."price" IS 'Цена на момент заказа (слепок)';
COMMENT ON COLUMN "order_item"."created_at" IS 'Дата создания';

CREATE INDEX "idx_order_item_order_id"
    ON "order_item" ("order_id");
CREATE INDEX "idx_order_item_product_id"
    ON "order_item" ("product_id");
CREATE INDEX "idx_order_item_order_product"
    ON "order_item" ("order_id", "product_id");

CREATE TABLE IF NOT EXISTS "payment" (
    -- Первичный ключ
                                         "id" BIGSERIAL NOT NULL,
    -- Заказ (ссылка на orders.id) — связь 1:1
                                         "order_id" BIGINT NOT NULL UNIQUE,
    -- Покупатель (ссылка на customer_profile.id)
                                         "customer_id" BIGINT NOT NULL,
    -- Сумма платежа (слепок на момент оплаты)
                                         "amount" DECIMAL(10,2) NOT NULL,
    -- Статус: PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED, CANCELLED
                                         "status" VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    -- Способ оплаты (слепок): CARD, WALLET, CASH_ON_DELIVERY
                                         "payment_method" VARCHAR(20) NOT NULL,
    -- ID транзакции в платежном шлюзе (Stripe, YooKassa и т.д.)
                                         "gateway_transaction_id" VARCHAR(255),
    -- Ключ идемпотентности (защита от дублирования платежей)
                                         "idempotency_key" VARCHAR(255) NOT NULL UNIQUE,
    -- Сообщение об ошибке (если платеж завершился с ошибкой)
                                         "error_message" TEXT,
    -- Дата создания платежа
                                         "created_at" TIMESTAMP NOT NULL DEFAULT NOW(),
    -- Дата последнего обновления
                                         "updated_at" TIMESTAMP NOT NULL DEFAULT NOW(),
    -- Дата завершения платежа (успех/ошибка)
                                         "completed_at" TIMESTAMP,
                                         PRIMARY KEY("id")
);

COMMENT ON TABLE "payment" IS 'Платежи по заказам';
COMMENT ON COLUMN "payment"."id" IS 'Первичный ключ';
COMMENT ON COLUMN "payment"."order_id" IS 'Заказ (ссылка на orders.id) — связь 1:1';
COMMENT ON COLUMN "payment"."customer_id" IS 'Покупатель (ссылка на customer_profile.id)';
COMMENT ON COLUMN "payment"."amount" IS 'Сумма платежа (слепок на момент оплаты)';
COMMENT ON COLUMN "payment"."status" IS 'Статус: PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED, CANCELLED';
COMMENT ON COLUMN "payment"."payment_method" IS 'Способ оплаты (слепок): CARD, WALLET, CASH_ON_DELIVERY';
COMMENT ON COLUMN "payment"."gateway_transaction_id" IS 'ID транзакции в платежном шлюзе (Stripe, YooKassa и т.д.)';
COMMENT ON COLUMN "payment"."idempotency_key" IS 'Ключ идемпотентности (защита от дублирования платежей)';
COMMENT ON COLUMN "payment"."error_message" IS 'Сообщение об ошибке (если платеж завершился с ошибкой)';
COMMENT ON COLUMN "payment"."created_at" IS 'Дата создания платежа';
COMMENT ON COLUMN "payment"."updated_at" IS 'Дата последнего обновления';
COMMENT ON COLUMN "payment"."completed_at" IS 'Дата завершения платежа (успех/ошибка)';

CREATE INDEX "idx_payment_order_id"
    ON "payment" ("order_id");
CREATE INDEX "idx_payment_customer_id"
    ON "payment" ("customer_id");
CREATE INDEX "idx_payment_status"
    ON "payment" ("status");
CREATE UNIQUE INDEX "idx_payment_idempotency_key"
    ON "payment" ("idempotency_key");
CREATE INDEX "idx_payment_gateway_transaction"
    ON "payment" ("gateway_transaction_id");
CREATE INDEX "idx_payment_created_at"
    ON "payment" ("created_at");
CREATE INDEX "idx_payment_status_created"
    ON "payment" ("status", "created_at");

CREATE TABLE IF NOT EXISTS "notification" (
    -- Первичный ключ
                                              "id" BIGSERIAL NOT NULL,
    -- Получатель (ссылка на customer_profile.id)
                                              "customer_id" BIGINT NOT NULL,
    -- Заказ (ссылка на orders.id) — опционально
                                              "order_id" BIGINT,
    -- Тип уведомления: ORDER_CONFIRMED, ORDER_PAID, ORDER_SHIPPED, ORDER_DELIVERED, ORDER_CANCELLED, PASSWORD_RESET, WELCOME, PROMO
                                              "type" VARCHAR(50) NOT NULL,
    -- Канал: EMAIL, SMS, PUSH
                                              "channel" VARCHAR(20) NOT NULL,
    -- Тема (для EMAIL)
                                              "subject" VARCHAR(255),
    -- Текст сообщения
                                              "content" TEXT NOT NULL,
    -- Статус: PENDING, SENT, DELIVERED, FAILED, READ
                                              "status" VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    -- Время отправки
                                              "sent_at" TIMESTAMP,
    -- Время доставки (подтверждение от провайдера)
                                              "delivered_at" TIMESTAMP,
    -- Время прочтения (для EMAIL/PUSH)
                                              "read_at" TIMESTAMP,
    -- Сообщение об ошибке (если отправка не удалась)
                                              "error_message" TEXT,
    -- Дата создания уведомления
                                              "created_at" TIMESTAMP NOT NULL DEFAULT NOW(),
    -- Дата последнего обновления
                                              "updated_at" TIMESTAMP NOT NULL DEFAULT NOW(),
                                              PRIMARY KEY("id")
);

COMMENT ON TABLE "notification" IS 'История уведомлений пользователей';
COMMENT ON COLUMN "notification"."id" IS 'Первичный ключ';
COMMENT ON COLUMN "notification"."customer_id" IS 'Получатель (ссылка на customer_profile.id)';
COMMENT ON COLUMN "notification"."order_id" IS 'Заказ (ссылка на orders.id) — опционально';
COMMENT ON COLUMN "notification"."type" IS 'Тип уведомления: ORDER_CONFIRMED, ORDER_PAID, ORDER_SHIPPED, ORDER_DELIVERED, ORDER_CANCELLED, PASSWORD_RESET, WELCOME, PROMO';
COMMENT ON COLUMN "notification"."channel" IS 'Канал: EMAIL, SMS, PUSH';
COMMENT ON COLUMN "notification"."subject" IS 'Тема (для EMAIL)';
COMMENT ON COLUMN "notification"."content" IS 'Текст сообщения';
COMMENT ON COLUMN "notification"."status" IS 'Статус: PENDING, SENT, DELIVERED, FAILED, READ';
COMMENT ON COLUMN "notification"."sent_at" IS 'Время отправки';
COMMENT ON COLUMN "notification"."delivered_at" IS 'Время доставки (подтверждение от провайдера)';
COMMENT ON COLUMN "notification"."read_at" IS 'Время прочтения (для EMAIL/PUSH)';
COMMENT ON COLUMN "notification"."error_message" IS 'Сообщение об ошибке (если отправка не удалась)';
COMMENT ON COLUMN "notification"."created_at" IS 'Дата создания уведомления';
COMMENT ON COLUMN "notification"."updated_at" IS 'Дата последнего обновления';

CREATE INDEX "idx_notification_customer_id"
    ON "notification" ("customer_id");
CREATE INDEX "idx_notification_order_id"
    ON "notification" ("order_id");
CREATE INDEX "idx_notification_type"
    ON "notification" ("type");
CREATE INDEX "idx_notification_channel"
    ON "notification" ("channel");
CREATE INDEX "idx_notification_status"
    ON "notification" ("status");
CREATE INDEX "idx_notification_created_at"
    ON "notification" ("created_at");
CREATE INDEX "idx_notification_customer_type"
    ON "notification" ("customer_id", "type");
CREATE INDEX "idx_notification_customer_created"
    ON "notification" ("customer_id", "created_at");

ALTER TABLE "delivery_address"
    ADD FOREIGN KEY("customer_id") REFERENCES "customer_profile"("id")
        ON UPDATE NO ACTION ON DELETE CASCADE;
ALTER TABLE "payment_method"
    ADD FOREIGN KEY("customer_id") REFERENCES "customer_profile"("id")
        ON UPDATE NO ACTION ON DELETE CASCADE;
ALTER TABLE "category"
    ADD FOREIGN KEY("parent_id") REFERENCES "category"("id")
        ON UPDATE NO ACTION ON DELETE SET NULL;
ALTER TABLE "product"
    ADD FOREIGN KEY("category_id") REFERENCES "category"("id")
        ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE "cart"
    ADD FOREIGN KEY("customer_id") REFERENCES "customer_profile"("id")
        ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE "cart"
    ADD FOREIGN KEY("product_id") REFERENCES "product"("id")
        ON UPDATE NO ACTION ON DELETE CASCADE;
ALTER TABLE "orders"
    ADD FOREIGN KEY("delivery_address_id") REFERENCES "delivery_address"("id")
        ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE "orders"
    ADD FOREIGN KEY("payment_method_id") REFERENCES "payment_method"("id")
        ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE "order_item"
    ADD FOREIGN KEY("order_id") REFERENCES "orders"("id")
        ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE "order_item"
    ADD FOREIGN KEY("product_id") REFERENCES "product"("id")
        ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE "payment"
    ADD FOREIGN KEY("order_id") REFERENCES "orders"("id")
        ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE "payment"
    ADD FOREIGN KEY("customer_id") REFERENCES "customer_profile"("id")
        ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE "notification"
    ADD FOREIGN KEY("order_id") REFERENCES "orders"("id")
        ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE "notification"
    ADD FOREIGN KEY("customer_id") REFERENCES "customer_profile"("id")
        ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE "orders"
    ADD FOREIGN KEY("customer_id") REFERENCES "customer_profile"("id")
        ON UPDATE NO ACTION ON DELETE NO ACTION;

CREATE OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS $$
BEGIN
    NEW."updated_at" = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Добавить для всех таблиц с updated_at
CREATE TRIGGER "trigger_customer_profile_updated_at"
    BEFORE UPDATE ON "customer_profile"
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER "trigger_delivery_address_updated_at"
    BEFORE UPDATE ON "delivery_address"
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER "trigger_payment_method_updated_at"
    BEFORE UPDATE ON "payment_method"
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER "trigger_product_updated_at"
    BEFORE UPDATE ON "product"
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER "trigger_category_updated_at"
    BEFORE UPDATE ON "category"
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER "trigger_cart_updated_at"
    BEFORE UPDATE ON "cart"
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER "trigger_orders_updated_at"
    BEFORE UPDATE ON "orders"
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER "trigger_payment_updated_at"
    BEFORE UPDATE ON "payment"
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER "trigger_notification_updated_at"
    BEFORE UPDATE ON "notification"
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Статусы заказов
ALTER TABLE "orders" ADD CONSTRAINT "chk_orders_status"
    CHECK ("status" IN ('CREATED', 'PAYMENT_PENDING', 'PAID', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'REFUNDED'));

-- Статусы платежей
ALTER TABLE "payment" ADD CONSTRAINT "chk_payment_status"
    CHECK ("status" IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'REFUNDED', 'CANCELLED'));

-- Статусы уведомлений
ALTER TABLE "notification" ADD CONSTRAINT "chk_notification_status"
    CHECK ("status" IN ('PENDING', 'SENT', 'DELIVERED', 'FAILED', 'READ'));

-- Типы способов оплаты
ALTER TABLE "payment_method" ADD CONSTRAINT "chk_payment_method_type"
    CHECK ("type" IN ('CARD', 'WALLET', 'GOOGLE_PAY', 'APPLE_PAY'));

-- Каналы уведомлений
ALTER TABLE "notification" ADD CONSTRAINT "chk_notification_channel"
    CHECK ("channel" IN ('EMAIL', 'SMS', 'PUSH'));