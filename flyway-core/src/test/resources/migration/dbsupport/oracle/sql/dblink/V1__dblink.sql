--
--         http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

declare
  dup_dblink_name exception;
  pragma exception_init(dup_dblink_name, -2011);
begin
  execute immediate q'[
    CREATE DATABASE LINK TEST_DBLINK
    CONNECT TO REMOTE_USER IDENTIFIED BY R3m0t3_pa$$w0rd
    USING 'REMOTE_DB'
  ]';
exception
  when dup_dblink_name then null;
end;
/
