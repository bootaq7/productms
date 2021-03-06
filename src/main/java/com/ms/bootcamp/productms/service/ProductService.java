package com.ms.bootcamp.productms.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.ms.bootcamp.productms.model.DiscountRequest;
import com.ms.bootcamp.productms.model.DiscountResponse;
import com.ms.bootcamp.productms.model.Product;
import com.ms.bootcamp.productms.model.ProductDTO;
import com.ms.bootcamp.productms.repo.ProductRepository;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

@Component
public class ProductService {

	@Autowired
	ProductRepository repo;

	@Autowired
	DiscoveryClient discoveryClient;

	@Autowired
	LoadBalancerClient loadBalancerClient;

	@Bean
	@LoadBalanced
	public RestTemplate createRestTemplate() {
		return new RestTemplate();
	}

	@Autowired
	RestTemplate restTemplate;

	@Autowired
	Discountms discountms;

	public List<Product> getAllProducts() {
		return repo.findAll();
	}

	public Product getProductById(Integer id) {
		Optional<Product> op = repo.findById(id);
		if (op.isPresent())
			return op.get();
		else
			return null;
	}

	public ProductDTO calculateDiscountv0(Product p) {
		DiscountRequest discountRequest = createDiscountRequest(p);

		List<ServiceInstance> serviceInstances = discoveryClient.getInstances("discountms");
		if (serviceInstances != null && serviceInstances.size() > 0) {
			for (ServiceInstance instance : serviceInstances) {
				System.out.println(instance.getHost() + ":" + instance.getPort());
			}
			ServiceInstance instance = serviceInstances.get(0);
			String url = "http://" + instance.getHost() + ":" + instance.getPort() + "/caldisc";
			System.out.println("Calling :" + url);
			RestTemplate restTemplate = new RestTemplate();
			HttpEntity<DiscountRequest> dRequest = new HttpEntity<DiscountRequest>(discountRequest);
			DiscountResponse dResponse = restTemplate.postForEntity(url, dRequest, DiscountResponse.class).getBody();
			return ceateProductResponseDTO(dResponse, p);
		}
		return null;

	}

	public ProductDTO calculateDiscountv1(Product p) {
		DiscountRequest discountRequest = createDiscountRequest(p);

		ServiceInstance instance = loadBalancerClient.choose("discountms");
		if (instance != null) {

			String url = "http://" + instance.getHost() + ":" + instance.getPort() + "/caldisc";
			System.out.println("Calling :" + url);
			RestTemplate restTemplate = new RestTemplate();
			HttpEntity<DiscountRequest> dRequest = new HttpEntity<DiscountRequest>(discountRequest);
			DiscountResponse dResponse = restTemplate.postForEntity(url, dRequest, DiscountResponse.class).getBody();
			return ceateProductResponseDTO(dResponse, p);
		}
		return null;

	}

	public ProductDTO calculateDiscountv2(Product p) {
		DiscountRequest discountRequest = createDiscountRequest(p);
		HttpEntity<DiscountRequest> dRequest = new HttpEntity<DiscountRequest>(discountRequest);
		DiscountResponse dResponse = restTemplate
				.postForEntity("http://discountms/caldisc", dRequest, DiscountResponse.class).getBody();
		return ceateProductResponseDTO(dResponse, p);

	}

	@HystrixCommand(fallbackMethod = "discountFallback")
	public ProductDTO calculateDiscountv3(Product p) {
		DiscountRequest discountRequest = createDiscountRequest(p);
		HttpEntity<DiscountRequest> dRequest = new HttpEntity<DiscountRequest>(discountRequest);
		DiscountResponse dResponse = restTemplate
				.postForEntity("http://discountms/caldisc", dRequest, DiscountResponse.class).getBody();
		return ceateProductResponseDTO(dResponse, p);

	}

	public ProductDTO calculateDiscountv4(Product p) {
		DiscountRequest discountRequest = createDiscountRequest(p);
		DiscountResponse dResponse = discountms.calculateDiscount(discountRequest);
		return ceateProductResponseDTO(dResponse, p);

	}

	// Strategy as decided by business!
	public ProductDTO discountFallback(Product p) {

		ProductDTO pdto = new ProductDTO();
		pdto.setCategory(p.getCategory());
		pdto.setDrp(p.getMrp());
		pdto.setFixedCategoryDiscount(0.0);
		pdto.setOnSpotDiscount(0.0);
		pdto.setId(p.getId());
		pdto.setMrp(p.getMrp());
		pdto.setName(p.getName());
		pdto.setShortDescription(p.getShortDescription());
		pdto.setTags(p.getTags());

		return pdto;
	}

	private DiscountRequest createDiscountRequest(Product p) {
		return new DiscountRequest(p.getCategory(), p.getMrp());
	}

	private ProductDTO ceateProductResponseDTO(DiscountResponse discountResponse, Product p) {
		ProductDTO pdto = new ProductDTO();
		pdto.setCategory(p.getCategory());
		pdto.setDrp(discountResponse.getDrp());
		pdto.setFixedCategoryDiscount(discountResponse.getFixedCategoryDiscount());
		pdto.setOnSpotDiscount(discountResponse.getOnSpotDiscount());
		pdto.setId(p.getId());
		pdto.setMrp(p.getMrp());
		pdto.setName(p.getName());
		pdto.setShortDescription(p.getShortDescription());
		pdto.setTags(p.getTags());

		return pdto;
	}

}
