package de.unistuttgart.t2.ui.webui.app;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import de.unistuttgart.t2.common.CartContent;
import de.unistuttgart.t2.common.OrderRequest;
import de.unistuttgart.t2.common.Product;
import de.unistuttgart.t2.ui.webui.domain.ItemToAdd;
import de.unistuttgart.t2.ui.webui.domain.PaymentDetails;

/**
 * Defines the http enpoints of the UI.
 * 
 * @author maumau
 *
 */
@Controller
public class UIController {
    
    private final Logger LOG = LoggerFactory.getLogger(getClass());
    
    @Autowired
    private RestTemplate template;
    
    private final String urlUiBackend;
    
    private final String urlProductsAll;
    private final String urlProductsAdd;
    private final String urlProductsDelete;
    private final String urlCart;
    private final String urlConfirm;
    
    
    public UIController(@Value("${t2.uibackend.url}") String urlUiBackend) {
        this.urlUiBackend = urlUiBackend;
        
        // must not set before base url is set!!!
        urlProductsAll = urlUiBackend + "products/all";
        urlProductsAdd = urlUiBackend + "products/add";
        urlProductsDelete = urlUiBackend + "products/delete";
        urlCart = urlUiBackend + "cart";
        urlConfirm = urlUiBackend + "confirm";
    }
    
    ////// PAGES TO REALLY LOOK AT ///////////
    
    @GetMapping("/ui/")
    public String index(Model model) {
        model.addAttribute("title", "T2 Store");
        return "index";
    }
    
    @GetMapping("/ui/products")
    public String products(Model model) {
        model.addAttribute("title", "Products");
        model.addAttribute("item", new ItemToAdd());
        
        List<Product> products = template.exchange(urlProductsAll, HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<List<Product>>(){}).getBody();
        
        model.addAttribute("productslist", products);
        
        return "category";
    }
    
    @GetMapping("/ui/cart")
    public String cart(Model model, HttpSession session) {
        model.addAttribute("title", "Cart");
        model.addAttribute("item", new ItemToAdd());
        
        LOG.info("SessionID : " + session.getId());
        
        // Header
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set(HttpHeaders.COOKIE, session.getId());
        
        // Request and send
        RequestEntity<Void> request = new RequestEntity<Void>(responseHeaders, HttpMethod.GET, URI.create(urlCart));        
        ResponseEntity<List<Product>> response = template.exchange(request, new ParameterizedTypeReference<List<Product>>(){});
        
        //Set View
        List<Product> products = response.getBody();
        model.addAttribute("OrderItems", products);
        
        return "cart";
    }

    @GetMapping("/ui/confirm")
    public String confirm(Model model, HttpSession session) {
        
        model.addAttribute("title", "Confirm");
        model.addAttribute("details", new PaymentDetails ());
        
        return "order";        
    }
    
   
    ////////// ACTIONS /////////////
   
    @PostMapping("/ui/add")
    public String add(@ModelAttribute("item") ItemToAdd item, HttpSession session) {
         
        LOG.info("SessionID : " + session.getId());
        LOG.info("Item to Add : " + item.toString());
        
        // Header
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set(HttpHeaders.COOKIE, session.getId());
        
        // Body
        CartContent body = new CartContent(Map.of(item.getProductId(), item.getUnits()));
        
        // Request and send
        RequestEntity<CartContent> request = new RequestEntity<CartContent>(body, responseHeaders, HttpMethod.POST, URI.create(urlProductsAdd));        
        ResponseEntity<List<Product>> response = template.exchange(request, new ParameterizedTypeReference<List<Product>>(){});
        
        LOG.info(response.getBody().toString());
        
        return "product";
    }
       
    @PostMapping("/ui/delete")
    public RedirectView delete(@ModelAttribute("item") ItemToAdd item, RedirectAttributes redirectAttributes, HttpSession session) {
         
        LOG.info("SessionID : " + session.getId());
        LOG.info("Item to Delete : " + item.toString());
        
        // Header
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set(HttpHeaders.COOKIE, session.getId());
        
        // Body
        CartContent body = new CartContent(Map.of(item.getProductId(), item.getUnits()));
        
        // Request and send
        RequestEntity<CartContent> request = new RequestEntity<CartContent>(body, responseHeaders, HttpMethod.POST, URI.create(urlProductsDelete));        
        template.exchange(request, Void.class);
       
        //TODO redirect : to display deleted products
        
        final RedirectView redirectView = new RedirectView("/ui/cart", true);
        //redirectAttributes.addFlashAttribute("title", "");
        return redirectView;
    }

    
    @PostMapping("/ui/confirm")
    public String confirm(@ModelAttribute("details") PaymentDetails details, Model model, HttpSession session) {
        LOG.info("SessionID : " + session.getId());
        
        // Header
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set(HttpHeaders.COOKIE, session.getId());
        
        //Body
        OrderRequest body = new OrderRequest(details.getCardNumber(), details.getCardOwner(), details.getChecksum());
        
        // Request and send
        RequestEntity<OrderRequest> request = new RequestEntity<OrderRequest>(body, responseHeaders, HttpMethod.POST, URI.create(urlConfirm));        
        template.exchange(request, void.class);
       
        //Set view
        model.addAttribute("title", "ConfirmING");

        // TODO : Display confirmation message :) / (or Failure) 
        
        return "category";        
    }
    
    
    
    
    /////////// FOR LEARNING PUPROSE - DELETE LATER ///////////////
    
    @GetMapping("/ui/test")
    public String test(HttpSession session, Model model) {
        
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("sessionid", session.getId());
        responseHeaders.set(HttpHeaders.COOKIE, session.getId());
        
        model.addAttribute("givenCookie", session.getId());
        
        RequestEntity<String> request = new RequestEntity<String>(responseHeaders, HttpMethod.GET, URI.create(urlUiBackend));
        
        ResponseEntity<String> response = template.exchange(request, String.class);
        
        HttpHeaders headers = response.getHeaders();
        String setCookie = headers.getFirst(HttpHeaders.SET_COOKIE);
        
        model.addAttribute("response", response.getBody());
        model.addAttribute("setCookie", setCookie);
            
        return "test-view"; 
    }
}
