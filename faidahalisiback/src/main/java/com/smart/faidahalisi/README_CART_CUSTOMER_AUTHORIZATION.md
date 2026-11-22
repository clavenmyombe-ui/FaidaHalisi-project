# FaidaHalisi: Customer, Cart, JWT na Authorization

Mwongozo huu unaeleza mabadiliko yaliyofanywa na namna ya kuyajaribu. Code imewekewa comments karibu na sehemu muhimu ili ufuatilie sababu ya kila ukaguzi.

## 1. Customer na User sasa zimeunganishwa

- `User` ni akaunti inayotumia email na password kuingia.
- `Customer` ni profile ya mnunuzi: namba ya simu na eneo.
- `Customer.user_id` inaunganisha profile na akaunti moja ya `User`.
- `Cart.customer_id` inaunganisha cart na profile ya mteja.
- JWT inatoa email ya akaunti iliyothibitishwa; backend haitumii `customerId` iliyotumwa na frontend kuamua umiliki.

### Usajili wa mteja

`POST /api/users`

```json
{
  "firstName": "Asha",
  "lastName": "Juma",
  "email": "asha@example.com",
  "password": "weka-password-imara-hapa",
  "role": "CUSTOMER"
}
```

Role inayoruhusiwa na registration ni `CUSTOMER` au `SELLER`. `ADMIN` haiwezi kuchaguliwa kupitia registration ya kawaida. Password inahifadhiwa kwa BCrypt.

Baada ya kuingia na kupata JWT, mteja anatengeneza profile yake:

`POST /api/customers/me`

```json
{
  "phone": "0712345678",
  "location": "Buguruni, Dar es Salaam"
}
```

Weka token kwenye header `Authorization: Bearer <token>`. Jina la akaunti linatoka kwenye `User`; mteja hawezi kuunganisha profile na akaunti ya mtu mwingine kwa kutuma `userId`.

## 2. Cart API mpya

Endpoints hizi zinahitaji JWT na profile ya `CUSTOMER` iliyoundwa.

- `POST /api/carts/me` — tengeneza cart ikiwa haipo, au rudisha iliyopo.
- `GET /api/carts/me` — pata cart ya akaunti yako.
- `GET /api/carts/me/items` — pata bidhaa zilizo ndani ya cart yako.
- `DELETE /api/carts/me/items` — ondoa bidhaa zote ndani ya cart bila kufuta cart.
- `DELETE /api/carts/me` — futa cart yako.
- `POST /api/cart-items/cart/{cartId}/product/{productId}?quantity=1` — ongeza bidhaa; backend hukagua kwamba cart ni yako na stock inatosha.
- `PUT /api/cart-items/{cartItemId}/decrease` — punguza quantity ya bidhaa iliyo kwenye cart yako.
- `DELETE /api/cart-items/{cartItemId}` — ondoa bidhaa moja kutoka cart yako.

Ingawa baadhi ya URL zina ID, backend hukagua umiliki kabla ya kuruhusu mabadiliko. Usalama haupaswi kutegemea kuficha ID kwenye frontend.

## 3. Kuweka oda

`POST /api/orders/cart/{cartId}`

```json
{
  "deliveryAddress": "Buguruni, Dar es Salaam"
}
```

Tuma JWT ya mteja. Backend huthibitisha kwamba cart inamilikiwa na akaunti hiyo, huhesabu bei kutoka database, hukagua stock na kuunda oda. Kwa toleo hili la MVP, cart moja inaruhusiwa kuwa na bidhaa za duka moja tu; bidhaa kutoka maduka tofauti zinahitaji oda tofauti.

- `GET /api/orders/mine` — oda za mteja aliyeingia.
- `GET /api/orders/{orderId}` — mteja wa oda, seller wa duka au admin pekee.
- `GET /api/orders/number/{orderNumber}` — mteja wa oda, seller wa duka au admin pekee.
- `GET /api/orders/store/{storeId}` — seller wa duka husika au admin pekee.
- `PUT /api/orders/{orderId}/status` — seller wa duka husika au admin pekee.
- `DELETE /api/orders/{orderId}` — mteja wa oda, seller wa duka au admin; sheria ya status bado inatumika.

## 4. API nilizoondoa kwa sasa

1. `GET /api/customers` — ilikuwa inarudisha wateja wote. Hii si salama kwa taarifa binafsi na haihitajiki kwa mteja wa kawaida.
2. `GET /api/customers/{id}` — iliruhusu kutafuta profile kwa ID badala ya profile ya akaunti iliyoingia.
3. `GET /api/customers/phone` na `GET /api/customers/location` — utafutaji huu hauhitajiki kwenye ununuzi wa msingi na unaweza kufichua taarifa za wateja.
4. `PUT /api/customers/{id}` na `DELETE /api/customers/{id}` — viliruhusu kuchagua profile ya kubadilisha/kufuta kwa ID. Vimebadilishwa na `PUT /api/customers/me`.
5. `GET /api/users/{id}`, `GET /api/users/email`, `PUT /api/users/{id}`, `DELETE /api/users/{id}` — vilikuwa vinaleta au kubadilisha akaunti kwa ID/email. Kwa sasa controller ya users inabaki na registration pekee; login iko `/api/auth/login`.
6. `GET /api/products/name?name=...` — imeondolewa kwa sababu majina ya bidhaa yanaweza kurudiwa na utafutaji huo hauhitajiki kwenye mtiririko wa msingi. Tumia bidhaa za duka na utafutaji wa frontend kwa sasa.
7. Controller nzima ya `/api/order-items/**` — imeondolewa kwa sababu order yenyewe inahusisha items zake; endpoints za items zilikuwa zinaongeza njia za ziada za kusoma data ya oda bila faida ya msingi.
8. Controller ya `/api/payments/**` — imeondolewa kwa muda. Controller ya awali ilikuwa na constructor iliyokosewa na endpoint ya `process` ilitumia `@PathVariable orderId` bila kuwa na `{orderId}` kwenye URL. Zaidi ya hayo, payment flow bado haina uthibitisho halisi kutoka payment provider. Payment model/service zimeachwa kwa integration inayofuata, lakini hakuna payment HTTP endpoint inayowashwa kwa sasa.

## 5. Mabadiliko muhimu ya bug

- Checkout sasa huhifadhi `Orders` kabla ya kuhifadhi `OrderItems`, ili `OrderItems.order_id` irejee order iliyopo.
- Checkout hufuta CartItem rows zote za cart husika. Code ya zamani ilitumia `deleteById(cartId)`, ikichukulia cart ID kama CartItem ID.
- Order hutumia bei na stock kutoka database, si jumla au bei inayotumwa na frontend.
- JSON ya bidhaa/oda imewekewa `@JsonIgnore` kwenye back-references muhimu ili kuzuia serialization loops.
- Customer profile haikubali `userId` kutoka frontend; account link inachukuliwa kutoka JWT.

## 6. Database iliyopo

Model mpya ina column `customer.user_id` yenye unique constraint. Ikiwa `spring.jpa.hibernate.ddl-auto=update` imewashwa, Hibernate inaweza kujaribu kuongeza column hiyo wakati app inaanza. Usitumie `create` au `create-drop` kwenye database yenye data unayotaka kuhifadhi.

Customer records za zamani zitakuwa na `user_id = NULL` mpaka ziunganishwe kwa njia iliyothibitishwa. Usiziunganishe kiotomatiki kwa namba ya simu bila uthibitisho wa umiliki. Kwa database inayosimamiwa kwa migration, ongeza column na foreign key kulingana na majina halisi ya table/column yaliyopo kwenye database yako.

## 7. Kile cha kuangalia kwenye `pom.xml` na `application.properties`

ZIP ya awali haikuwa na `pom.xml` wala `application.properties`, kwa hiyo faili hizo hazikuweza kuhaririwa au project ku-run build hapa. Hakikisha project ina:

- `spring-boot-starter-web`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-security`
- `spring-boot-starter-validation`
- MySQL driver
- `io.jsonwebtoken:jjwt-api`, `jjwt-impl` (runtime), `jjwt-jackson` (runtime)
- Lombok ikiwa bado unatumia annotations zake

Tumia Java/Spring Boot versions zinazoendana na project yako iliyopo; usibadilishe versions bila kuangalia parent/BOM ya sasa. `application.properties` inahitaji database URL, username/password, `app.security.jwt.secret=${JWT_SECRET}`, `app.security.jwt.expiration-ms=3600000`, na `app.cors.allowed-origins=http://localhost:4200` (badilisha origin kama Angular inatumia port nyingine). JWT secret iwe environment variable, si secret iliyoandikwa wazi kwenye Git.

## 8. Majaribio ya lazima kabla ya kuunganisha frontend

1. Sajili `CUSTOMER`, login, tengeneza `/api/customers/me`, kisha `/api/carts/me`.
2. Jaribu kupata cart bila token: inapaswa kukataa kwa `401`.
3. Tengeneza seller mwingine na cart/customer tofauti; jaribu kutumia cart ID ya mwingine. Inapaswa kukataa kwa `403`.
4. Jaribu kuongeza quantity kubwa kuliko stock: inapaswa kukataa.
5. Weka oda, hakikisha `orders`, `order_items`, stock na cart vinabadilika kama inavyotarajiwa.
6. Ingia kama seller B na ujaribu kubadilisha store/product/order ya seller A. Inapaswa kukataa.
7. Hakikisha customer hawezi kubadilisha role yake kuwa `ADMIN` kupitia registration.

**Kikomo cha uthibitishaji:** Nimekagua na kuunganisha source files, lakini siwezi kusema build imepita kwa sababu ZIP iliyotumwa haikuwa na `pom.xml`, configuration ya database, au tests. Baada ya kuweka project kwenye mazingira yako, endesha `mvn test` na ulete error kamili ikiwa ipo.
