#include <iostream>
#include <vector>
#include <map>
#include <set>
#include <math.h>
#include <openssl/sha.h>

using namespace std;

class Node;
int get_sha_hash(string file_name);

class FingerTable {
public:
	vector<Node*> fingerTable;
	Node* local_node;
	int nodeId;

	FingerTable(int id, Node* node) {
		this->nodeId = id;
		this->local_node = node;
	}

	~FingerTable() {
		this->fingerTable.clear();
	}

	void printFingerTable(int);
};

class Node {

public:
	uint64_t id;
	Node *predecessor, *successor;
	std::map<int, string> local_keys;
	FingerTable *fingertable;
	string ip_address, port_number;

	Node(int id, string ip_address, string port_number) {
		this->id = (int) id;
		this->predecessor = NULL;
		this->fingertable = new FingerTable(this->id, this);
		this->ip_address = ip_address;
		this->port_number = port_number;
	}

	// Move keys (if any) to the newly added node
	void moveKeys(Node* succ, int new_node_id) {
		map<int, string> m;
		map<int, string>::iterator iter;

		for (map<int, string>::iterator iter = succ->local_keys.begin();
				iter != succ->local_keys.end(); iter++) {
			if (iter->first <= new_node_id
					&& iter->first > succ->predecessor->id) {
				insert_key_local(iter->second, iter->first);
			} else {
				m.insert(pair<int, string>(iter->first, iter->second));
			}
		}

		succ->local_keys.clear();
		succ->local_keys = m;
	}

	// Node join operation
	void join(Node* node) {
		if (node == NULL) {  // First node to join
			for (int i = 0; i < 8; i++) {
				fingertable->fingerTable.push_back(this);
			}
			predecessor = this;
		} else {
			for (int i = 0; i < 8; i++) {
				fingertable->fingerTable.push_back(this);
			}

			// Find successor to attach to
			Node* succ = node->find_successor(id);

			// Update node's successor to point to the successor
			fingertable->fingerTable[0] = succ;

			// Update predecessor's successor to self
			succ->predecessor->fingertable->fingerTable[0] = this;

			// Update predecessor to successor's old predecessor
			predecessor = succ->predecessor;

			// move keys on the successor before changing predecessor
			moveKeys(succ, id);

			// Update successor's predecssor to self
			succ->predecessor = this;

			// update finger table
			// fingerTable[0] is always the successor
			createFingerTable();
		}
	}

	// creates the finger table
	void createFingerTable() {
		for (int i = 1; i < fingertable->fingerTable.size(); i++) {
			Node* ptr = this;
			int flag = 0;

			for (int j = 0; j < pow(2, i); j++) {
				ptr = ptr->fingertable->fingerTable[0];

				if (ptr == this) {
					flag = 1;
					break;
				}
			}

			if (flag == 0) {
				fingertable->fingerTable[i] = ptr;
			}
		}
	}

	// stabilize the finger tables
	void stabilize() {
		for (int i = 1; i < fingertable->fingerTable.size(); i++) {
			fingertable->fingerTable[i]->createFingerTable();
		}
	}

	// Find Successor
	Node* find_successor(int id) {
		if (this->id == id) {
			return this;
		} else if (this->id > id) {
			return this;
		} else {
			return fingertable->fingerTable[0]->find_successor(
					fingertable->fingerTable[0]->id);
		}
	}

	// Search a key value pair
	string find_key(string key) {
		string node_id;
		string ret_val;

		cout << "\nSearching Key " << key << " on node " << id << endl;
		int hash = get_sha_hash(key);
		node_id = local_key_lookup(hash);
		if (node_id != "") {
			ret_val = "Found value - " + node_id + " on Node - "
					+ to_string(id) + "\n";
		} else {
			for (int i = 0; i < fingertable->fingerTable.size(); i++) {
				node_id = fingertable->fingerTable[i]->local_key_lookup(hash);
				if (node_id != "") {
					ret_val =  "Found value - " + node_id + " on Node - "
							+ to_string(fingertable->fingerTable[i]->id) + "\n";
					break;
				}
			}
		}

		return ret_val;
	}

	// Insert key
	void insert_key(string file_name) {
		int key = get_sha_hash(file_name); 

		Node* succ = this->fingertable->fingerTable[0];

		if (succ->id < id && id <= key) {
			succ->insert_key_local(file_name, key);
		} else if (predecessor->id > id && key > predecessor->id) {
			insert_key_local(file_name, key);
		} else {
			int cnt = 0;
			set<int> checked;
			while (succ->id < key && checked.find(succ->id) == checked.end()) {
				checked.insert(succ->id);
				succ = succ->fingertable->fingerTable[0];
				cnt++;
			}
			succ->insert_key_local(file_name, key);
		}
	}

	// Insert a key on this node
	void insert_key_local(string file_name, int key) {
		local_keys.insert(pair<int, string>(key, file_name));
	}

	// Search a key locally
	string local_key_lookup(int key) {
		string node = "";

		for (int i = 0; i < local_keys.size(); i++)
			if (local_keys.find(key)->first == key)
				node =  local_keys.find(key)->second;

		return node;
	}

	// print predecessor and successor
	void print_predecessor_successor() {
		cout<<"\nNode "<< this->id << " Predecessor: " 
			<< this->predecessor->id << " Successor: " << this->successor->id<<endl;
	}

	// print ip address and id
	void print_ip_id() {
		cout<<"Node " << this->id << " with ip: "<< this->ip_address<<" port: "<<this->port_number<<endl;
	}

	// print the list of files on this node
	void print_files() {
		cout << "\n**** Node ID : " << this->id << " ****";
		for(auto itr: local_keys) {
			cout<<itr.second<<endl;
		}
	}
};

// Print Finger Table
void FingerTable::printFingerTable(int pred) {
	cout << "\n**** Node ID : " << this->nodeId << " ****";
	cout << "\nFingerTable\n";

	for (int i = 0; i < fingerTable.size(); i++) {
		if (i == 0 || (nodeId != fingerTable[i]->fingertable->nodeId)) {
			cout << i + 1 << " : " << fingerTable[i]->fingertable->nodeId
					<< "\n";
		}
	}

	cout << "\nKeys : ";
	for (map<int, string>::iterator iter = local_node->local_keys.begin();
			iter != local_node->local_keys.end(); iter++) {
		cout << iter->second << "  ";
	}

	cout << "\n**********************\n";
}

int get_sha_hash(string file_name) {
	unsigned char hash[SHA_DIGEST_LENGTH]; // == 20

	SHA1((const unsigned char *)file_name.c_str(), sizeof(file_name.c_str()) - 1, hash);

	int num = 0;
	for (int i = 0; i < 5; ++i)
	{
		num += (hash[i]);
	}

	return num%32;
}

int main() {
	Node* n0 = new Node(1, "127.09.34.112", "8000");
	Node* n1 = new Node(5, "123.22.24.23", "8458");
	Node* n2 = new Node(31, "231.239.67.255", "4532");
	Node* n3 = new Node(16, "231.249.17.245", "4512");
	n0 -> print_ip_id();
	n1 -> print_ip_id();
	n2 -> print_ip_id();

	// n0 join
	n0->join(NULL);
	cout << "\nn0 joins the Chord network\n";

	n0->insert_key("first.txt");
	n0->insert_key("second.txt");
	n0->fingertable->printFingerTable(n0->predecessor->id);
	cout << "\n\n";

	// n1 join
	n1->join(n0);

	// stabilize
	n1->stabilize();
	n0->stabilize();
	cout << "\nn1 joins the Chord network\n";

	n1->insert_key("third.txt");
	n1->insert_key("fourth.txt");
	n1->insert_key("fifth.txt");
	n1->insert_key("sixth.txt");
	n0->insert_key("seventh.txt");
	n0->fingertable->printFingerTable(n0->predecessor->id);
	n1->fingertable->printFingerTable(n1->predecessor->id);
	cout << "\n\n";

	// n2 join
	n2->join(n0);

	// stabilize
	n0->stabilize();
	n1->stabilize();

	cout << "\nn2 joins the Chord network\n";
	n0->insert_key("random1.txt");
	n1->insert_key("random2.txt");
	n2->insert_key("random3.txt");
	n1->insert_key("random5.txt");
	n1->insert_key("random6.txt");
	n0->insert_key("random19.txt");

	n0->fingertable->printFingerTable(n0->predecessor->id);
	n1->fingertable->printFingerTable(n1->predecessor->id);
	n2->fingertable->printFingerTable(n2->predecessor->id);
	cout << "\n\n";

	n3->join(n2);
	cout<<"\nn3 joins the network\n";
	n0->fingertable->printFingerTable(n0->predecessor->id);
	n1->fingertable->printFingerTable(n1->predecessor->id);
	n2->fingertable->printFingerTable(n2->predecessor->id);
	n3->fingertable->printFingerTable(n3->predecessor->id);


	// Random search for values on non-local nodes i.e nodes that may/may not contain the
	// keys being searched for, locally on them
	cout << n0->find_key("first.txt") << endl;
	cout << n1->find_key("fifth.txt") << endl;
	cout << n2->find_key("sixth.txt") << endl;
	return 0;
}
