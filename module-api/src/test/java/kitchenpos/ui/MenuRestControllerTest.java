package kitchenpos.ui;

import kitchenpos.application.MenuQueryService;
import kitchenpos.application.MenuService;
import kitchenpos.common.exception.InvalidPriceException;
import kitchenpos.domain.menu.Menu;
import kitchenpos.domain.menu.MenuCreate;
import kitchenpos.domain.menu.MenuProduct;
import kitchenpos.dto.request.MenuCreateRequest;
import kitchenpos.dto.request.MenuProductCreateRequest;
import kitchenpos.dto.response.MenuViewResponse;
import kitchenpos.fixture.CleanUp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static kitchenpos.fixture.MenuFixture.*;
import static kitchenpos.ui.JsonUtil.toJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MenuRestController.class)
@ExtendWith(MockitoExtension.class)
class MenuRestControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MenuService menuService;

    @MockBean
    private MenuQueryService menuQueryService;

    @BeforeEach
    void setUp() throws Exception {
        CleanUp.cleanUp();
    }

    @Test
    @DisplayName("[post]/api/menus - 메뉴의 가격이 비어 있거나, 0원보다 적을경우 BadRequest이다.")
    void 메뉴의_가격이_비어_있거나_0원보다_적을경우_BadRequest이다() throws Exception {
        // given
        MenuCreateRequest menuCreateRequest = new MenuCreateRequest("Menu",
                BigDecimal.valueOf(-1),
                1L,
                Arrays.asList());

        given(menuService.create(any(MenuCreate.class))).willAnswer(i -> i.getArgument(0));

        // when
        MvcResult mvcResult = mockMvc.perform(
                post("/api/menus")
                        .content(toJson(menuCreateRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        // then
        assertThat(mvcResult.getResolvedException()).isInstanceOf(InvalidPriceException.class);
    }

    @Test
    @DisplayName("[post]/api/menus - 정상정인 메뉴 등록")
    void 정상적인_메뉴_등록() throws Exception {
        // given
        MenuCreateRequest menuCreateRequest = new MenuCreateRequest("Menu", BigDecimal.valueOf(1), 1L,
                Arrays.asList(
                        new MenuProductCreateRequest(1L, 1L, 1L),
                        new MenuProductCreateRequest(2L, 2L, 2L)
                )
        );

        given(menuService.create(any(MenuCreate.class))).willReturn(양념치킨_콜라_1000원_1개.getId());
        given(menuQueryService.findById(양념치킨_콜라_1000원_1개.getId()))
                .willReturn(MenuViewResponse.of(양념치킨_콜라_1000원_1개, 양념치킨_콜라_1000원_1개_MenuProduct));

        // when & then
        mockMvc.perform(
                post("/api/menus")
                        .content(toJson(menuCreateRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(validateMenu("$", 양념치킨_콜라_1000원_1개))
                .andExpect(validateMenuProducts("$.menuProducts[0]", 양념치킨_콜라_1000원_1개_MenuProduct.get(0)))
                .andExpect(validateMenuProducts("$.menuProducts[1]", 양념치킨_콜라_1000원_1개_MenuProduct.get(1)))
                .andReturn();
    }

    @Test
    @DisplayName("[get]/api/menus - 정상적인 리스트 조회")
    void 정상적인_리스트_조회() throws Exception {
        List<Menu> menus = Arrays.asList(양념치킨_콜라_1000원_1개, 양념치킨_콜라_1000원_2개);
        List<MenuViewResponse> menuResponses = Arrays.asList(
                MenuViewResponse.of(menus.get(0), 양념치킨_콜라_1000원_1개_MenuProduct),
                MenuViewResponse.of(menus.get(1), 양념치킨_콜라_1000원_2개_MenuProduct)
        );
        given(menuQueryService.list()).willReturn(menuResponses);

        // when & then
        mockMvc.perform(
                get("/api/menus"))
                .andExpect(status().isOk())
                .andExpect(validateMenu("$[0]", menus.get(0)))
                .andExpect(validateMenuProducts("$[0].menuProducts[0]", 양념치킨_콜라_1000원_1개_MenuProduct.get(0)))
                .andExpect(validateMenuProducts("$[0].menuProducts[1]", 양념치킨_콜라_1000원_1개_MenuProduct.get(1)))
                .andExpect(validateMenuProducts("$[1].menuProducts[0]", 양념치킨_콜라_1000원_2개_MenuProduct.get(0)))
                .andExpect(validateMenuProducts("$[1].menuProducts[1]", 양념치킨_콜라_1000원_2개_MenuProduct.get(1)))
                .andReturn();

    }

    private ResultMatcher validateMenu(String expressionPrefix, Menu menu) {
        return result -> {
            ResultMatcher.matchAll(
                    jsonPath(expressionPrefix + ".id").value(menu.getId()),
                    jsonPath(expressionPrefix + ".name").value(menu.getName().toString()),
                    jsonPath(expressionPrefix + ".price").value(menu.getPrice().toBigDecimal()),
                    jsonPath(expressionPrefix + ".menuGroupId").value(menu.getMenuGroup().getId())
            ).match(result);
        };
    }

    private ResultMatcher validateMenuProducts(String expressionPrefix, MenuProduct menuProduct) {
        return result -> {
            ResultMatcher.matchAll(
                    jsonPath(expressionPrefix + ".seq").value(menuProduct.getSeq()),
                    jsonPath(expressionPrefix + ".menuId").value(menuProduct.getMenu().getId()),
                    jsonPath(expressionPrefix + ".productId").value(menuProduct.getProduct().getId()),
                    jsonPath(expressionPrefix + ".quantity").value(menuProduct.getQuantity().toLong())
            ).match(result);
        };
    }
}